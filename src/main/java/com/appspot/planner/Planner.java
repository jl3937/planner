package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

@Path("plan")
public class Planner {
  public static final int DEFAULT_SEARCH_RADIUS = 50000;  // in meters
  public static final int LOOKUP_COUNT = 5;

  @POST
  public String getPlan(String jsonRequest) {
    // Parse request
    GetPlanRequest request;
    try {
      GetPlanRequest.Builder requestBuilder = GetPlanRequest.newBuilder();
      JsonFormat.merge(jsonRequest, requestBuilder);
      request = requestBuilder.build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
      return "Invalid request.";
    }

    // Check input
    if (request.getRequirement().hasMinPriceLevel() && request.getRequirement().hasMaxPriceLevel() &&
        request.getRequirement().getMinPriceLevel() > request.getRequirement().getMaxPriceLevel()) {
      return "Invalid request. min_price_level should be less than or equal to max_price_level.";
    }

    // Get time and location
    Calendar calendar;
    if (request.hasTimeZone()) {
      calendar = Calendar.getInstance(TimeZone.getTimeZone(request.getTimeZone()));
    } else {
      // By default, use system time zone.
      calendar = Calendar.getInstance();
    }
    if (request.getRequirement().getTimePeriod().getStartTime().hasValue()) {
      calendar.setTimeInMillis(request.getRequirement().getTimePeriod().getStartTime().getValue());
    } else {
      /*
      calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
      calendar.set(Calendar.HOUR_OF_DAY, 8);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      */
    }

    // Get candidates for each event
    GetPlanResponse.Builder response = GetPlanResponse.newBuilder();
    for (Event event : request.getEventList()) {
      response.addProcessedEvent(processEvent(event, request.getRequirement(), calendar));
    }

    // Get available schedule.
    Scheduler scheduler = new Scheduler(request, response, calendar);
    response = scheduler.getSchedule();

    // Generate response string.
    if (response.hasSchedule()) {
      response.clearScheduleCandidate();
      response.clearProcessedEvent();
    }
    String responseJson = JsonFormat.printToString(response.build());
    JsonParser parser = new JsonParser();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonElement el = parser.parse(responseJson);
    return gson.toJson(el);
  }

  private Event processEvent(Event event, Requirement requirement, Calendar calendar) {
    Location location = requirement.getStartLoc();
    int radius = requirement.hasRadius() ? requirement.getRadius() : DEFAULT_SEARCH_RADIUS;
    Event.Builder processedEvent = Event.newBuilder().mergeFrom(event);
    int day = calendar.get(Calendar.DAY_OF_WEEK);
    long time = calendar.getTimeInMillis();
    if (event.getType() == Event.Type.PLACE || event.getType() == Event.Type.FOOD) {
      location = GoogleGeoAPI.getLocation(location);
      if (location == null) {
        return null;
      }
      PlaceResult placeResult = GoogleGeoAPI.searchPlace(event.getContent(), location, radius, event.getType());
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      int minute = calendar.get(Calendar.MINUTE);
      int hourMinute = hour * 100 + minute;
      int lookupCount = 0;
      for (Place place : placeResult.getResultsList()) {
        if (lookupCount++ == LOOKUP_COUNT) {
          break;
        }
        place = GoogleGeoAPI.getPlaceDetail(place.getPlaceId()).getResult();
        if (place.hasOpeningHours()) {
          for (Place.OpeningHours.Period period : place.getOpeningHours().getPeriodsList()) {
            int startTime = -1;
            int endTime = -1;
            if (period.getOpen().getDay() == 0 && period.getOpen().getTime().equals("0000") && !period.hasClose()) {
              // Open 7 days 24 hours
              startTime = hourMinute;
              endTime = 2359;
            } else if (period.getOpen().getDay() == day - 1) {
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
              TimeSlot.Builder candidate = TimeSlot.newBuilder();
              candidate.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeByHourMinute(startTime,
                  time, hourMinute, calendar)).setEndTime(Util.getTimeByHourMinute(endTime, time, hourMinute,
                  calendar));
              candidate.getEventBuilder().setContent(place.getName()).setType(event.getType());
              candidate.getSpecBuilder().getStartLocBuilder().setCoordinate(place.getGeometry().getLocation())
                  .setAddress(place.getFormattedAddress());
              candidate.getSpecBuilder().setRating(place.getRating()).setPriceLevel(place.getPriceLevel());
              candidate.getSpecBuilder().addAllTypes(place.getTypesList());
              candidate.getSpecBuilder().setSuggestedDuration(Config.getInstance().getSuggestedDuration(candidate));
              // candidate.setPlace(place);
              processedEvent.addCandicates(candidate);
            }
          }
        }
      }
    } else if (event.getType() == Event.Type.MOVIE) {
      int dayOfToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
      ArrayList<Movie> movieResults = GoogleMovieCrawler.searchMovie(event.getContent(), location, day - dayOfToday,
          calendar);
      for (Movie movie : movieResults) {
        for (Theater theater : movie.getTheatersList()) {
          if (theater.getTimesCount() == 0 || theater.getTimes(0).getValue() < time) {
            continue;
          }
          Location startLocation = Location.newBuilder().setAddress(theater.getAddress()).build();
          startLocation = GoogleGeoAPI.getLocation(startLocation);
          for (Time startTime : theater.getTimesList()) {
            TimeSlot.Builder candidate = TimeSlot.newBuilder();
            candidate.getEventBuilder().setContent(theater.getName()).setType(event.getType());
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
}
