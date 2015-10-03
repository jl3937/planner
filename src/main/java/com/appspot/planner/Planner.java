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
    }

    // Get candidates for each event
    GetPlanResponse.Builder response = GetPlanResponse.newBuilder();
    for (Event event : request.getEventList()) {
      response.addProcessedEvent(processEvent(event, request.getRequirement().getStartLoc(), calendar));
    }

    // Get available schedule.
    Scheduler scheduler = new Scheduler(request, response, calendar);
    response = scheduler.getSchedule();

    /*
    long time = calendar.getTimeInMillis();
    int dayOfToday = calendar.get(Calendar.DAY_OF_WEEK);
    Location previousLoc = request.getRequirement().getStartLoc();
    for (Event event : request.getEventList()) {
      Location eventLoc = null;
      String eventContent = "";
      Place selectedPlace = null;
      Movie selectedMovie = null;
      long duration = 0;
      long eventStartTime = 0;
      calendar.setTimeInMillis(time);
      int day = calendar.get(Calendar.DAY_OF_WEEK);
      if (event.getType() == Event.Type.PLACE || event.getType() == Event.Type.FOOD) {
        Location location = GoogleGeoAPI.getLocation(previousLoc);
        if (location == null) {
          return "Invalid location: " + previousLoc;
        }
        PlaceResult placeResult = GoogleGeoAPI.searchPlace(event.getContent(), location, DEFAULT_SEARCH_RADIUS, event
            .getType());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int hourMinute = hour * 100 + minute;
        for (Place result : placeResult.getResultsList()) {
          selectedPlace = GoogleGeoAPI.getPlaceDetail(result.getPlaceId()).getResult();
          boolean open = false;
          if (selectedPlace.hasOpeningHours()) {
            for (Place.OpeningHours.Period period : selectedPlace.getOpeningHours().getPeriodsList()) {
              if (period.getOpen().getDay() == day - 1) {
                int openTime = Integer.parseInt(period.getOpen().getTime());
                int closeTime = Integer.parseInt(period.getClose().getTime());
                if (openTime <= hourMinute && hourMinute < closeTime) {
                  open = true;
                  break;
                } else if (hourMinute < openTime) {
                  open = true;
                  eventStartTime = time + TimeUnit.MILLISECONDS.convert((openTime / 100 - hourMinute / 100), TimeUnit
                      .HOURS) +
                      TimeUnit.MILLISECONDS.convert((openTime % 100 - hourMinute % 100), TimeUnit.MINUTES);
                  break;
                }
              }
            }
            if (!open) {
              continue;
            }
          }
          eventLoc = Location.newBuilder().setAddress(selectedPlace.getFormattedAddress()).build();
          eventContent = selectedPlace.getName();
          if (event.getType() == Event.Type.FOOD) {
            duration = DEFAULT_FOOD_TIME;
          } else {
            duration = DEFAULT_PLACE_TIME;
          }
          break;
        }
      } else if (event.getType() == Event.Type.MOVIE) {
        ArrayList<Movie> movieResults = GoogleMovieCrawler.searchMovie(event.getContent(), previousLoc, day -
            dayOfToday, calendar);
        for (Movie movie : movieResults) {
          for (Theater theater : movie.getTheatersList()) {
            if (theater.getTimesCount() == 0 || theater.getTimes(0).getValue() < time) {
              continue;
            }
            eventLoc = Location.newBuilder().setAddress(theater.getAddress()).build();
            eventContent = theater.getName();
            selectedMovie = movie;
            eventStartTime = theater.getTimes(0).getValue();
            duration = movie.getDuration().getValue();
            break;
          }
        }
      }

      if (eventLoc == null) {
        continue;
      }

      DistanceMatrixResult result = GoogleGeoAPI.getDuration(previousLoc, eventLoc, request.getRequirement()
          .getTravelMode().name().toLowerCase());
      System.out.println(Util.getEstimatedDuration(previousLoc, eventLoc).getValue());
      Transit selectedTransit = null;
      for (Transit transit : result.getRows(0).getElementsList()) {
        selectedTransit = transit;
        break;
      }

      TimeSlot.Builder timeSlot = TimeSlot.newBuilder();
      timeSlot.getEventBuilder().setType(Event.Type.TRANSPORT);
      Spec.Builder spec = timeSlot.getSpecBuilder();
      TimePeriod.Builder timePeriod = spec.getTimePeriodBuilder();
      timePeriod.setStartTime(Util.getTimeFromTimestamp(time, calendar));
      time += TimeUnit.MILLISECONDS.convert(selectedTransit.getDuration().getValue(), TimeUnit.SECONDS);
      timePeriod.setEndTime(Util.getTimeFromTimestamp(time, calendar));
      spec.setStartLoc(previousLoc).setEndLoc(eventLoc).setTravelMode(request.getRequirement().getTravelMode());
      timeSlot.setTransit(selectedTransit);
      response.addSchedule(timeSlot.build());


      timeSlot = TimeSlot.newBuilder();
      timeSlot.getEventBuilder().setContent(eventContent).setType(event.getType());
      spec = timeSlot.getSpecBuilder();
      timePeriod = spec.getTimePeriodBuilder();
      if (eventStartTime != 0) {
        time = eventStartTime;
      }
      timePeriod.setStartTime(Util.getTimeFromTimestamp(time, calendar));
      time += duration;
      timePeriod.setEndTime(Util.getTimeFromTimestamp(time, calendar));
      spec.setStartLoc(eventLoc).setEndLoc(eventLoc);
      if (selectedPlace != null) {
        // timeSlot.setPlace(selectedPlace);
      }
      if (selectedMovie != null) {
        // timeSlot.setMovie(selectedMovie);
      }
      response.addSchedule(timeSlot.build());
      previousLoc = eventLoc;
    }

    Location endLoc;
    if (request.getRequirement().hasEndLoc()) {
      endLoc = request.getRequirement().getEndLoc();
    } else {
      endLoc = request.getRequirement().getStartLoc();
    }
    DistanceMatrixResult result = GoogleGeoAPI.getDuration(previousLoc, endLoc, request.getRequirement()
        .getTravelMode().name().toLowerCase());
    Transit selectedTransit = null;
    for (Transit transit : result.getRows(0).getElementsList()) {
      selectedTransit = transit;
      break;
    }

    if (response.getScheduleCount() == 0) {
      return "No available schedule.";
    }

    TimeSlot.Builder timeSlot = TimeSlot.newBuilder();
    timeSlot.getEventBuilder().setType(Event.Type.TRANSPORT);
    Spec.Builder spec = timeSlot.getSpecBuilder();
    TimePeriod.Builder timePeriod = spec.getTimePeriodBuilder();
    timePeriod.setStartTime(Util.getTimeFromTimestamp(time, calendar));
    time += TimeUnit.MILLISECONDS.convert(selectedTransit.getDuration().getValue(), TimeUnit.SECONDS);
    timePeriod.setEndTime(Util.getTimeFromTimestamp(time, calendar));
    spec.setStartLoc(previousLoc).setEndLoc(endLoc).setTravelMode(request.getRequirement().getTravelMode());
    timeSlot.setTransit(selectedTransit);
    response.addSchedule(timeSlot);
    */
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

  private Event processEvent(Event event, Location location, Calendar calendar) {
    Event.Builder processedEvent = Event.newBuilder().mergeFrom(event);
    int day = calendar.get(Calendar.DAY_OF_WEEK);
    long time = calendar.getTimeInMillis();
    if (event.getType() == Event.Type.PLACE || event.getType() == Event.Type.FOOD) {
      location = GoogleGeoAPI.getLocation(location);
      if (location == null) {
        return null;
      }
      PlaceResult placeResult = GoogleGeoAPI.searchPlace(event.getContent(), location, DEFAULT_SEARCH_RADIUS, event
          .getType());
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
            if (period.getOpen().getDay() == day - 1) {
              int openTime = Integer.parseInt(period.getOpen().getTime());
              int closeTime = Integer.parseInt(period.getClose().getTime());
              if (openTime <= hourMinute && hourMinute < closeTime) {
                TimeSlot.Builder candidate = TimeSlot.newBuilder();
                candidate.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeByHourMinute(hourMinute,
                    time, hourMinute, calendar)).setEndTime(Util.getTimeByHourMinute(closeTime, time, hourMinute,
                    calendar));
                candidate.getEventBuilder().setContent(place.getName()).setType(event.getType());
                candidate.getSpecBuilder().getStartLocBuilder().setCoordinate(place.getGeometry().getLocation())
                    .setAddress(place.getFormattedAddress());
                candidate.getSpecBuilder().setRating(place.getRating()).setPriceLevel(place.getPriceLevel());
                // candidate.setPlace(place);
                processedEvent.addCandicates(candidate);
              } else if (hourMinute < openTime) {
                TimeSlot.Builder candidate = TimeSlot.newBuilder();
                candidate.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeByHourMinute(openTime,
                    time, hourMinute, calendar)).setEndTime(Util.getTimeByHourMinute(closeTime, time, hourMinute,
                    calendar));
                candidate.getEventBuilder().setContent(place.getName()).setType(event.getType());
                candidate.getSpecBuilder().getStartLocBuilder().setCoordinate(place.getGeometry().getLocation())
                    .setAddress(place.getFormattedAddress());
                candidate.getSpecBuilder().setRating(place.getRating()).setPriceLevel(place.getPriceLevel());
                // candidate.setPlace(place);
                processedEvent.addCandicates(candidate);
              }
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
