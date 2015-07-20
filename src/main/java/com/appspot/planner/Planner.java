package com.appspot.planner;

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

  public Planner() {
    this.yelpAPI = new YelpAPI();
  }

  @ApiMethod(name = "planner.get_plan", httpMethod = "post")
  public Message getPlan(Message request) {
    String result = this.yelpAPI.search("Sushi", "San Mateo, CA");
    Message response = new Message();
    response.schedule = new Message.Slot[1];
    response.schedule[0] = new Message.Slot();
    response.schedule[0].event = new Message.Event();
    response.schedule[0].event.content = result;
    return response;
  }

  @ApiMethod(name = "planner.authed", path = "planner/authed")
  public Message authedPlanner(User user) {
    Message response = new Message();
    return response;
  }
}
