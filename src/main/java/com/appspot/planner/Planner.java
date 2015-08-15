package com.appspot.planner;

import com.appspot.planner.model.DistanceMatrixResult;
import com.appspot.planner.model.Message;
import com.appspot.planner.model.Movie;
import com.appspot.planner.model.PlaceDetailResult;
import com.appspot.planner.model.PlaceResult;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.users.User;

import java.util.ArrayList;

import javax.inject.Named;

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
  YelpAPI yelpAPI;
  GoogleGeoAPI googleGeoAPI;
  GoogleMovieCrawler googleMovieCrawler;

  public Planner() {
    this.yelpAPI = new YelpAPI();
    this.googleGeoAPI = new GoogleGeoAPI();
    this.googleMovieCrawler = new GoogleMovieCrawler();
  }

  @ApiMethod(name = "planner.get_plan", httpMethod = "post")
  public Message getPlan(Message request) {
    Message response = new Message();
    String previousLoc = request.requirement.startLoc;
    if (request.requirement.travelMode == null) {
      request.requirement.travelMode = Message.Spec.TravelMode.DRIVING;
    }
    for (Message.Event event : request.events) {
      String eventLoc = "";
      String eventContent = "";
      String eventLength = "";
      if (event.type == null) {
        event.type = Message.Event.Type.FOOD;
      }
      if (event.type == Message.Event.Type.FOOD) {
        PlaceResult placeResult = this.googleGeoAPI.searchPlace(event.content,
                                                                previousLoc);
        for (PlaceResult.Result result : placeResult.results) {
          if (result.openingHours.openNow == true) {
            String placeId = result.placeId;
            PlaceDetailResult placeDetailResult =
                this.googleGeoAPI.getPlaceDetail(placeId);
            eventLoc = result.formattedAddress;
            eventContent = result.name;
            eventLength = "1hr";
            break;
          }
        }
      } else if (event.type == Message.Event.Type.MOVIE) {
        Movie movie = this.googleMovieCrawler.searchMovie(event.content,
                                                          previousLoc).get(0);
        Movie.Theater theater = movie.theaters.get(0);
        eventLoc = theater.address;
        eventContent = theater.name;
        eventLength = movie.length;
      }

      DistanceMatrixResult result = this.googleGeoAPI.getDuration(
          previousLoc,
          eventLoc,
          request.requirement.travelMode.name().toLowerCase());
      String duration = result.rows.get(0).elements.get(0).duration.text;
      
      Message.TimeSlot timeSlot = new Message.TimeSlot();
      timeSlot.event.type = Message.Event.Type.TRANSPORT;
      timeSlot.spec.startLoc = previousLoc;
      timeSlot.spec.endLoc = eventLoc;
      timeSlot.spec.length = duration;
      timeSlot.spec.travelMode = request.requirement.travelMode;
      response.schedule.add(timeSlot);

      timeSlot = new Message.TimeSlot();
      timeSlot.event.content = eventContent;
      timeSlot.event.type = event.type;
      timeSlot.spec.startLoc = eventLoc;
      timeSlot.spec.length = eventLength;
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
    String duration = result.rows.get(0).elements.get(0).duration.text;
    
    Message.TimeSlot timeSlot = new Message.TimeSlot();
    timeSlot.event.type = Message.Event.Type.TRANSPORT;
    timeSlot.spec.startLoc = previousLoc;
    timeSlot.spec.endLoc = request.requirement.endLoc;
    timeSlot.spec.length = duration;
    timeSlot.spec.travelMode = request.requirement.travelMode;
    response.schedule.add(timeSlot);

    return response;
  }

  @ApiMethod(name = "planner.authed", path = "planner/authed")
  public Message authedPlanner(User user) {
    Message response = new Message();
    return response;
  }
}
