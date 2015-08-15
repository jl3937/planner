package com.appspot.planner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.nodes.Document;

import com.appspot.planner.model.Movie;
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
    for (Message.Event event : request.events) {
      String eventLoc = "";
      String eventContent = "";
      String eventLength = "";
      if (event.type == Message.Event.Type.FOOD) {
        JSONObject result = this.googleGeoAPI.searchPlace(event.content,
                                                          previousLoc);
        JSONArray results = (JSONArray) result.get("results");
        JSONObject firstResult = (JSONObject) results.get(0);
        eventLoc = firstResult.get("formatted_address").toString();
        eventContent = firstResult.get("name").toString();
        eventLength = "1hr";
      } else if (event.type == Message.Event.Type.MOVIE) {
        Movie movie = this.googleMovieCrawler.searchMovie(event.content,
                                                          previousLoc).get(0);
        Movie.Theater theater = movie.theaters.get(0);
        eventLoc = theater.address;
        eventContent = theater.name;
        eventLength = movie.length;
      }

      JSONObject matrix =  this.googleGeoAPI.getDuration(previousLoc, eventLoc);
      JSONArray rows = (JSONArray) matrix.get("rows");
      JSONObject firstRow = (JSONObject) rows.get(0);
      JSONArray elements = (JSONArray) firstRow.get("elements");
      JSONObject firstElement = (JSONObject) elements.get(0);
      JSONObject duration = (JSONObject) firstElement.get("duration");
      
      Message.TimeSlot timeSlot = new Message.TimeSlot();
      timeSlot.event.type = Message.Event.Type.TRANSPORT;
      timeSlot.spec.startLoc = previousLoc;
      timeSlot.spec.endLoc = eventLoc;
      timeSlot.spec.length = duration.get("text").toString();
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
    JSONObject matrix = 
        this.googleGeoAPI.getDuration(previousLoc, request.requirement.endLoc);
    JSONArray rows = (JSONArray) matrix.get("rows");
    JSONObject firstRow = (JSONObject) rows.get(0);
    JSONArray elements = (JSONArray) firstRow.get("elements");
    JSONObject firstElement = (JSONObject) elements.get(0);
    JSONObject duration = (JSONObject) firstElement.get("duration");
    
    Message.TimeSlot timeSlot = new Message.TimeSlot();
    timeSlot.event.content = duration.get("text").toString();
    timeSlot.spec.startLoc = previousLoc;
    timeSlot.spec.endLoc = request.requirement.endLoc;
    response.schedule.add(timeSlot);

    return response;
  }

  @ApiMethod(name = "planner.authed", path = "planner/authed")
  public Message authedPlanner(User user) {
    Message response = new Message();
    return response;
  }
}
