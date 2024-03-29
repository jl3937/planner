package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.Util;

import java.util.ArrayList;
import java.util.Calendar;

public class CandidateCollector {
  public static final int DEFAULT_SEARCH_RADIUS = 50000;  // in meters
  public static final int LOOKUP_COUNT = 5;

  private GetPlanRequest request;
  private Requirement requirement;
  private GetPlanResponse.Builder response;
  private Calendar calendar;

  CandidateCollector(GetPlanRequest request, GetPlanResponse.Builder response, Calendar calendar) {
    this.request = request;
    this.requirement = request.getRequirement();
    this.response = response;
    this.calendar = calendar;
  }

  void collectCandidate() {
    for (Event event : request.getEventList()) {
      response.addProcessedEvent(processEvent(event));
    }
  }

  private Event processEvent(Event event) {
    Location location = requirement.getStartLoc();
    int radius = requirement.hasRadius() ? requirement.getRadius() : DEFAULT_SEARCH_RADIUS;
    Event.Builder processedEvent = Event.newBuilder().mergeFrom(event);
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    long time = calendar.getTimeInMillis();
    location = GoogleGeoAPI.getLocation(location);
    if (location == null) {
      return null;
    }
    if (event.getType() == Event.Type.PLACE || event.getType() == Event.Type.FOOD) {
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      int minute = calendar.get(Calendar.MINUTE);
      int hourMinute = hour * 100 + minute;
      int lookupCount = 0;
      PlaceResult startPlaceResult = GoogleGeoAPI.searchPlace(event.getContent(), location, radius, event.getType());
      for (Place place : startPlaceResult.getResultsList()) {
        boolean avoid = false;
        for (String avoidId : event.getAvoidIdList()) {
          if (avoidId.equals(place.getPlaceId())) {
            avoid = true;
            break;
          }
        }
        if (avoid) {
          continue;
        }
        if (lookupCount++ == LOOKUP_COUNT) {
          break;
        }
        lookupPlace(place.getPlaceId(), dayOfWeek, hourMinute, processedEvent);
      }
      if (!requirement.getEndLoc().getAddress().equals("") && !requirement.getEndLoc().getAddress().equals
          (requirement.getStartLoc().getAddress())) {
        lookupCount = 0;
        PlaceResult endPlaceResult = GoogleGeoAPI.searchPlace(event.getContent(), location, radius, event.getType());
        for (Place place : endPlaceResult.getResultsList()) {
          if (lookupCount++ == LOOKUP_COUNT) {
            break;
          }
          lookupPlace(place.getPlaceId(), dayOfWeek, hourMinute, processedEvent);
        }
      }
    } else if (event.getType() == Event.Type.MOVIE) {
      int dayOfToday = Calendar.getInstance(calendar.getTimeZone()).get(Calendar.DAY_OF_WEEK);
      ArrayList<Movie> movieResults = GoogleMovieCrawler.searchMovie(event.getContent(), location, dayOfWeek -
          dayOfToday, calendar);
      for (Movie movie : movieResults) {
        for (Theater theater : movie.getTheatersList()) {
          if (theater.getTimesCount() == 0 || theater.getTimes(0).getValue() < time) {
            continue;
          }
          Location startLocation = Location.newBuilder().setAddress(theater.getAddress()).build();
          startLocation = GoogleGeoAPI.getLocation(startLocation);
          for (Time startTime : theater.getTimesList()) {
            TimeSlot.Builder candidate = TimeSlot.newBuilder();
            candidate.getEventBuilder().setContent(event.getContent()).setType(event.getType()).setResult(theater
                .getName());
            // candidate.setMovie(movie);
            candidate.getSpecBuilder().setStartLoc(startLocation);
            TimePeriod.Builder timePeriod = candidate.getSpecBuilder().getTimePeriodBuilder();
            timePeriod.setStartTime(startTime);
            timePeriod.setEndTime(Util.getTimeFromTimestamp(startTime.getValue() + movie.getDuration().getValue(),
                calendar));
            processedEvent.addCandicates(candidate);
          }
        }
      }
    }
    return processedEvent.build();
  }

  private void lookupPlace(String placeId, int dayOfWeek, int hourMinute, Event.Builder processedEvent) {
    Place place = GoogleGeoAPI.getPlaceDetail(placeId).getResult();
    if (place.hasRating() && requirement.hasMinRating() && requirement.getMinRating() > place.getRating()) {
      return;
    }
    if (place.hasPriceLevel()) {
      if (requirement.hasMinPriceLevel() && requirement.getMinPriceLevel() > place.getPriceLevel()) {
        return;
      }
      if (requirement.hasMaxPriceLevel() && requirement.getMaxPriceLevel() < place.getPriceLevel()) {
        return;
      }
    }
    if (place.hasOpeningHours()) {
      for (Place.OpeningHours.Period period : place.getOpeningHours().getPeriodsList()) {
        int startTime = -1;
        int endTime = -1;
        if (period.getOpen().getDay() == 0 && period.getOpen().getTime().equals("0000") && !period.hasClose()) {
          // Open 7 days 24 hours
          startTime = hourMinute;
          endTime = 2359;
        } else if (period.getOpen().getDay() == dayOfWeek - 1) {
          int openTime = Integer.parseInt(period.getOpen().getTime());
          int closeTime = Integer.parseInt(period.getClose().getTime());
          if (openTime <= hourMinute && hourMinute < closeTime) {
            startTime = hourMinute;
            endTime = closeTime;
          } else if (hourMinute < openTime) {
            startTime = openTime;
            endTime = closeTime;
          }
        }
        if (startTime != -1) {
          long time = calendar.getTimeInMillis();
          TimeSlot.Builder candidate = TimeSlot.newBuilder();
          candidate.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeByHourMinute(startTime, time,
              hourMinute, calendar)).setEndTime(Util.getTimeByHourMinute(endTime, time, hourMinute, calendar));
          candidate.getEventBuilder().setContent(processedEvent.getContent()).setType(processedEvent.getType())
              .setResult(place.getName());
          candidate.getSpecBuilder().getStartLocBuilder().setCoordinate(place.getGeometry().getLocation()).setAddress
              (place.getFormattedAddress());
          candidate.getSpecBuilder().setRating(place.getRating()).setPriceLevel(place.getPriceLevel());
          candidate.getSpecBuilder().addAllTypes(place.getTypesList());
          candidate.getSpecBuilder().setSuggestedDuration(Config.getInstance().getSuggestedDuration(candidate));
          candidate.setPlace(place);
          processedEvent.addCandicates(candidate);
        }
      }
    }
  }
}
