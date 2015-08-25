package com.appspot.planner;

import com.appspot.planner.model.*;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Api(
    name = "planner",
    version = "v1",
    scopes = {Constants.EMAIL_SCOPE},
    clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID, Constants
        .API_EXPLORER_CLIENT_ID},
    audiences = {Constants.ANDROID_AUDIENCE})
public class Planner {
  GoogleGeoAPI googleGeoAPI;
  GoogleMovieCrawler googleMovieCrawler;
  Calendar calendar;

  public static final long DEFAULT_PLACE_TIME = 600000;
  public static final long DEFAULT_FOOD_TIME = 3600000;

  public Planner() {
    this.googleGeoAPI = new GoogleGeoAPI();
    this.googleMovieCrawler = new GoogleMovieCrawler();
    this.calendar = Calendar.getInstance();
  }

  @ApiMethod(name = "planner.get_plan", httpMethod = "post")
  public Plan getPlan(GetPlanRequest request) {
    Plan response = new Plan();
    String previousLoc = request.requirement.startLoc;
    if (request.requirement.travelMode == null) {
      request.requirement.travelMode = Spec.TravelMode.DRIVING;
    }

    long time;
    if (request.requirement.startTime.value <= 0) {
      time = this.calendar.getTime().getTime();
    } else {
      time = request.requirement.startTime.value;
    }
    for (Event event : request.events) {
      String eventLoc = "";
      String eventContent = "";
      if (event.type == null) {
        event.type = Event.Type.PLACE;
      }
      Place selectedPlace = null;
      Movie selectedMovie = null;
      long duration = 0;
      long eventStartTime = 0;
      if (event.type == Event.Type.PLACE || event.type == Event.Type.FOOD) {
        PlaceResult placeResult = this.googleGeoAPI.searchPlace(event.content, previousLoc);
        Date date = new Date(time);
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int hourMinute = hour * 100 + minute;
        for (PlaceResult.Result result : placeResult.results) {
          selectedPlace = this.googleGeoAPI.getPlaceDetail(result.placeId).result;
          boolean open = false;
          if (selectedPlace.openingHours != null) {
            for (Place.OpeningHours.Period period : selectedPlace.openingHours.periods) {
              if (period.open.day == day - 1) {
                int openTime = Integer.parseInt(period.open.time);
                int closeTime = Integer.parseInt(period.close.time);
                if (openTime <= hourMinute && hourMinute < closeTime) {
                  open = true;
                  break;
                } else if (hourMinute < openTime) {
                  open = true;
                  eventStartTime = time +
                      TimeUnit.MILLISECONDS.convert((openTime - hourMinute) / 100, TimeUnit.HOURS) +
                      TimeUnit.MILLISECONDS.convert((openTime - hourMinute) % 100, TimeUnit.MINUTES);
                  break;
                }
              }
            }
            if (!open) {
              continue;
            }
          }
          eventLoc = result.formattedAddress;
          eventContent = result.name;
          if (event.type == Event.Type.FOOD) {
            duration = DEFAULT_FOOD_TIME;
          } else {
            duration = DEFAULT_PLACE_TIME;
          }
          break;
        }
      } else if (event.type == Event.Type.MOVIE) {
        ArrayList<Movie> movieResults = this.googleMovieCrawler.searchMovie(event.content, previousLoc);
        for (Movie movie : movieResults) {
          for (Movie.Theater theater : movie.theaters) {
            if (theater.times.size() == 0 || theater.times.get(0).value < time) {
              continue;
            }
            movie.theater = theater;
            movie.theaters.clear();
            eventLoc = theater.address;
            eventContent = theater.name;
            selectedMovie = movie;
            eventStartTime = theater.times.get(0).value;
            duration = movie.duration.value;
            break;
          }
        }
      }

      if (eventLoc.isEmpty()) {
        continue;
      }

      DistanceMatrixResult result = this.googleGeoAPI.getDuration(previousLoc, eventLoc, request.requirement
          .travelMode.name().toLowerCase());
      Transit selectedTransit = null;
      for (Transit transit : result.rows.get(0).elements) {
        selectedTransit = transit;
        break;
      }

      TimeSlot timeSlot = new TimeSlot();
      timeSlot.event.type = Event.Type.TRANSPORT;
      timeSlot.spec.startTime.value = time;
      timeSlot.spec.startTime.text = timeToString(time);
      time += TimeUnit.MILLISECONDS.convert(selectedTransit.duration.value, TimeUnit.SECONDS);
      timeSlot.spec.endTime.value = time;
      timeSlot.spec.endTime.text = timeToString(time);
      timeSlot.spec.startLoc = previousLoc;
      timeSlot.spec.endLoc = eventLoc;
      timeSlot.spec.travelMode = request.requirement.travelMode;
      timeSlot.transit = selectedTransit;
      response.schedule.add(timeSlot);

      timeSlot = new TimeSlot();
      timeSlot.event.content = eventContent;
      timeSlot.event.type = event.type;
      if (eventStartTime != 0) {
        time = eventStartTime;
      }
      timeSlot.spec.startTime.value = time;
      timeSlot.spec.startTime.text = timeToString(time);
      time += duration;
      timeSlot.spec.endTime.value = time;
      timeSlot.spec.endTime.text = timeToString(time);
      timeSlot.spec.startLoc = eventLoc;
      timeSlot.spec.endLoc = eventLoc;
      timeSlot.place = selectedPlace;
      timeSlot.movie = selectedMovie;
      response.schedule.add(timeSlot);

      previousLoc = eventLoc;
    }

    if (request.requirement.endLoc == null) {
      request.requirement.endLoc = request.requirement.startLoc;
    }
    DistanceMatrixResult result = this.googleGeoAPI.getDuration(previousLoc, request.requirement.endLoc, request
        .requirement.travelMode.name().toLowerCase());
    Transit selectedTransit = null;
    for (Transit transit : result.rows.get(0).elements) {
      selectedTransit = transit;
      break;
    }

    if (response.schedule.size() == 0) {
      return response;
    }

    TimeSlot timeSlot = new TimeSlot();
    timeSlot.event.type = Event.Type.TRANSPORT;
    timeSlot.spec.startTime.value = time;
    timeSlot.spec.startTime.text = timeToString(time);
    time += TimeUnit.MILLISECONDS.convert(selectedTransit.duration.value, TimeUnit.SECONDS);
    timeSlot.spec.endTime.value = time;
    timeSlot.spec.endTime.text = timeToString(time);
    timeSlot.spec.startLoc = previousLoc;
    timeSlot.spec.endLoc = request.requirement.endLoc;
    timeSlot.spec.travelMode = request.requirement.travelMode;
    timeSlot.transit = selectedTransit;
    response.schedule.add(timeSlot);

    return response;
  }

  private String timeToString(long timestamp) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mmaa");
    TimeZone timeZone = TimeZone.getTimeZone("PST");
    dateFormat.setTimeZone(timeZone);
    Date date = new Date();
    date.setTime(timestamp);
    return dateFormat.format(date);
  }
}
