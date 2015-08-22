package com.appspot.planner;

import com.appspot.planner.model.*;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Defines v1 of a helloworld API, which provides simple "greeting" methods.
 */
@Api(
    name = "planner",
    version = "v1",
    scopes = {Constants.EMAIL_SCOPE},
    clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
    audiences = {Constants.ANDROID_AUDIENCE}
)
public class Planner {
  GoogleGeoAPI googleGeoAPI;
  GoogleMovieCrawler googleMovieCrawler;
  Calendar calendar;

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

    long time = 0;
    if (request.requirement.startTime != 0) {
      time = this.calendar.getTime().getTime();
    } else {
      time = request.requirement.startTime;
    }
    for (Event event : request.events) {
      String eventLoc = "";
      String eventContent = "";
      String eventLength = "";
      if (event.type == null) {
        event.type = Event.Type.FOOD;
      }
      Place selectedPlace = null;
      Movie selectedMovie = null;
      long duration = 0;
      if (event.type == Event.Type.FOOD) {
        PlaceResult placeResult = this.googleGeoAPI.searchPlace(event.content,
                                                                previousLoc);
        for (PlaceResult.Result result : placeResult.results) {
          if (result.openingHours == null ||
              result.openingHours.openNow == true) {
            String placeId = result.placeId;
            selectedPlace = this.googleGeoAPI.getPlaceDetail(placeId).result;
            eventLoc = result.formattedAddress;
            eventContent = result.name;
            duration = Constants.FOOD_TIME_IN_SECOND * Constants.MILLI_PER_SECOND;
            break;
          }
        }
      } else if (event.type == Event.Type.MOVIE) {
        ArrayList<Movie> movieResults =
            this.googleMovieCrawler.searchMovie(event.content, previousLoc);
        for (Movie movie : movieResults) {
          Movie.Theater theater = movie.theaters.get(0);
          movie.theater = theater;
          movie.theaters.clear();
          eventLoc = theater.address;
          eventContent = theater.name;
          selectedMovie = movie;
          duration = movie.duration.value;
          break;
        }
      }

      DistanceMatrixResult result = this.googleGeoAPI.getDuration(
          previousLoc,
          eventLoc,
          request.requirement.travelMode.name().toLowerCase());
      Transit selectedTransit = null;
      for (Transit transit : result.rows.get(0).elements) {
        selectedTransit = transit;
        break;
      }
      
      TimeSlot timeSlot = new TimeSlot();
      timeSlot.event.type = Event.Type.TRANSPORT;
      timeSlot.spec.startTime = time;
      time += selectedTransit.duration.value * Constants.MILLI_PER_SECOND;
      timeSlot.spec.endTime = time;
      timeSlot.spec.startLoc = previousLoc;
      timeSlot.spec.endLoc = eventLoc;
      timeSlot.spec.travelMode = request.requirement.travelMode;
      timeSlot.transit = selectedTransit;
      response.schedule.add(timeSlot);

      timeSlot = new TimeSlot();
      timeSlot.event.content = eventContent;
      timeSlot.event.type = event.type;
      timeSlot.spec.startTime = time;
      time += duration;
      timeSlot.spec.startTime = time;
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
    DistanceMatrixResult result = this.googleGeoAPI.getDuration(
        previousLoc,
        request.requirement.endLoc,
        request.requirement.travelMode.name().toLowerCase());
    Transit selectedTransit = null;
    for (Transit transit : result.rows.get(0).elements) {
      selectedTransit = transit;
      break;
    }

    TimeSlot timeSlot = new TimeSlot();
    timeSlot.event.type = Event.Type.TRANSPORT;
    timeSlot.spec.startTime = time;
    time += selectedTransit.duration.value * Constants.MILLI_PER_SECOND;
    timeSlot.spec.endTime = time;
    timeSlot.spec.startLoc = previousLoc;
    timeSlot.spec.endLoc = request.requirement.endLoc;
    timeSlot.spec.travelMode = request.requirement.travelMode;
    timeSlot.transit = selectedTransit;
    response.schedule.add(timeSlot);

    return response;
  }
}
