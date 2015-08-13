package com.appspot.planner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

  public Planner() {
    this.yelpAPI = new YelpAPI();
    this.googleGeoAPI = new GoogleGeoAPI();
  }

  @ApiMethod(name = "planner.get_plan", httpMethod = "post")
  public Message getPlan(Message request) {
    Message response = new Message();
    for (Message.Event event : request.events) {
      JSONObject result = this.googleGeoAPI.searchPlace(
          event.content, request.requirement.startLoc);
      JSONArray results = (JSONArray) result.get("results");
      JSONObject firstResult = (JSONObject) results.get(0);
      String eventLoc = firstResult.get("formatted_address").toString();

      JSONObject matrix = 
          this.googleGeoAPI.getDuration(request.requirement.startLoc, eventLoc);
      JSONArray rows = (JSONArray) matrix.get("rows");
      JSONObject firstRow = (JSONObject) rows.get(0);
      JSONArray elements = (JSONArray) firstRow.get("elements");
      JSONObject firstElement = (JSONObject) elements.get(0);
      JSONObject duration = (JSONObject) firstElement.get("duration");
      Message.TimeSlot timeSlot = new Message.TimeSlot();
      timeSlot.event.content = duration.get("text").toString();
      timeSlot.spec.startLoc = request.requirement.startLoc;
      timeSlot.spec.endLoc = eventLoc;
      response.schedule.add(timeSlot);

      timeSlot = new Message.TimeSlot();
      timeSlot.event.content = firstResult.get("name").toString();
      timeSlot.spec.startLoc = eventLoc;
      response.schedule.add(timeSlot);
    }
    return response;
  }

  @ApiMethod(name = "planner.authed", path = "planner/authed")
  public Message authedPlanner(User user) {
    Message response = new Message();
    return response;
  }
}
