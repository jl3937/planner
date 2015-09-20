package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Path("plan")
public class Planner {
  GoogleGeoAPI googleGeoAPI;
  GoogleMovieCrawler googleMovieCrawler;

  public static final long DEFAULT_PLACE_TIME = 600000;
  public static final long DEFAULT_FOOD_TIME = 3600000;
  public static final int DEFAULT_RADIUS = 10000;  // in meters

  public Planner() {
    this.googleGeoAPI = new GoogleGeoAPI();
    this.googleMovieCrawler = new GoogleMovieCrawler();
  }

  @POST
  public String getPlan(String jsonRequest) {
    GetPlanRequest request;
    try {
      GetPlanRequest.Builder requestBuilder = GetPlanRequest.newBuilder();
      JsonFormat.merge(jsonRequest, requestBuilder);
      request = requestBuilder.build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
      return "Invalid request.";
    }

    Plan.Builder response = Plan.newBuilder();
    String previousLoc = request.getRequirement().getStartLoc();
    Calendar calendar;
    if (request.hasTimeZone()) {
      calendar = Calendar.getInstance(TimeZone.getTimeZone(request.getTimeZone()));
    } else {
      // By default, use system time zone.
      calendar = Calendar.getInstance();
    }
    int dayOfToday = calendar.get(Calendar.DAY_OF_WEEK);
    TimeZone timeZone = calendar.getTimeZone();
    long time;
    if (request.getRequirement().getStartTime().hasValue()) {
      time = request.getRequirement().getStartTime().getValue();
    } else {
      // By default, use current time.
      time = calendar.getTimeInMillis();
    }
    for (Event event : request.getEventList()) {
      String eventLoc = "";
      String eventContent = "";
      Place selectedPlace = null;
      Movie selectedMovie = null;
      long duration = 0;
      long eventStartTime = 0;
      calendar.setTimeInMillis(time);
      int day = calendar.get(Calendar.DAY_OF_WEEK);
      if (event.getType() == Event.Type.PLACE || event.getType() == Event.Type.FOOD) {
        Geometry.Location location = this.googleGeoAPI.getLocation(previousLoc);
        PlaceResult placeResult = this.googleGeoAPI.searchPlace(event.getContent(), location, DEFAULT_RADIUS, event
            .getType());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int hourMinute = hour * 100 + minute;
        for (Place result : placeResult.getResultsList()) {
          selectedPlace = this.googleGeoAPI.getPlaceDetail(result.getPlaceId()).getResult();
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
          eventLoc = result.getFormattedAddress();
          eventContent = result.getName();
          if (event.getType() == Event.Type.FOOD) {
            duration = DEFAULT_FOOD_TIME;
          } else {
            duration = DEFAULT_PLACE_TIME;
          }
          break;
        }
      } else if (event.getType() == Event.Type.MOVIE) {
        ArrayList<Movie> movieResults = this.googleMovieCrawler.searchMovie(event.getContent(), previousLoc, day -
            dayOfToday, calendar);
        for (Movie movie : movieResults) {
          for (Theater theater : movie.getTheatersList()) {
            if (theater.getTimesCount() == 0 || theater.getTimes(0).getValue() < time) {
              continue;
            }
            eventLoc = theater.getAddress();
            eventContent = theater.getName();
            selectedMovie = movie;
            eventStartTime = theater.getTimes(0).getValue();
            duration = movie.getDuration().getValue();
            break;
          }
        }
      }

      if (eventLoc.isEmpty()) {
        continue;
      }

      DistanceMatrixResult result = this.googleGeoAPI.getDuration(previousLoc, eventLoc, request.getRequirement()
          .getTravelMode().name().toLowerCase());
      Transit selectedTransit = null;
      for (Transit transit : result.getRows(0).getElementsList()) {
        selectedTransit = transit;
        break;
      }

      TimeSlot.Builder timeSlot = TimeSlot.newBuilder();
      timeSlot.getEventBuilder().setType(Event.Type.TRANSPORT);
      timeSlot.getSpecBuilder().setStartTime(Time.newBuilder().setValue(time).setText(timeToString(time, timeZone)));
      time += TimeUnit.MILLISECONDS.convert(selectedTransit.getDuration().getValue(), TimeUnit.SECONDS);
      timeSlot.getSpecBuilder().setEndTime(Time.newBuilder().setValue(time).setText(timeToString(time, timeZone)))
          .setStartLoc(previousLoc).setEndLoc(eventLoc).setTravelMode(request.getRequirement().getTravelMode());
      timeSlot.setTransit(selectedTransit);
      response.addSchedule(timeSlot.build());

      timeSlot = TimeSlot.newBuilder();
      timeSlot.getEventBuilder().setContent(eventContent).setType(event.getType());
      if (eventStartTime != 0) {
        time = eventStartTime;
      }

      timeSlot.getSpecBuilder().getStartTimeBuilder().setValue(time).setText(timeToString(time, timeZone));
      time += duration;
      timeSlot.getSpecBuilder().getEndTimeBuilder().setValue(time).setText(timeToString(time, timeZone));
      timeSlot.getSpecBuilder().setStartLoc(eventLoc).setEndLoc(eventLoc);
      if (selectedPlace != null) {
        timeSlot.setPlace(selectedPlace);
      }
      if (selectedMovie != null) {
        timeSlot.setMovie(selectedMovie);
      }
      response.addSchedule(timeSlot.build());
      previousLoc = eventLoc;
    }

    String endLoc;
    if (request.getRequirement().hasEndLoc()) {
      endLoc = request.getRequirement().getEndLoc();
    } else {
      endLoc = request.getRequirement().getStartLoc();
    }
    DistanceMatrixResult result = this.googleGeoAPI.getDuration(previousLoc, endLoc, request.getRequirement()
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
    timeSlot.getSpecBuilder().getStartTimeBuilder().setValue(time).setText(timeToString(time, timeZone));
    time += TimeUnit.MILLISECONDS.convert(selectedTransit.getDuration().getValue(), TimeUnit.SECONDS);
    timeSlot.getSpecBuilder().getEndTimeBuilder().setValue(time).setText(timeToString(time, timeZone));
    timeSlot.getSpecBuilder().setStartLoc(previousLoc).setEndLoc(endLoc).setTravelMode(request.getRequirement()
        .getTravelMode());
    timeSlot.setTransit(selectedTransit);
    response.addSchedule(timeSlot);

    String responseJson = JsonFormat.printToString(response.build());
    JsonParser parser = new JsonParser();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonElement el = parser.parse(responseJson);
    return gson.toJson(el);
  }

  private String timeToString(long timestamp, TimeZone timeZone) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mmaa");
    dateFormat.setTimeZone(timeZone);
    Date date = new Date();
    date.setTime(timestamp);
    return dateFormat.format(date);
  }
}
