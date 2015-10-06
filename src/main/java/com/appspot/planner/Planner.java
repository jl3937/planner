package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.Calendar;
import java.util.TimeZone;

@Path("plan")
public class Planner {
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
    TimeZone timeZone = GoogleGeoAPI.getTimeZone(request.getRequirement().getStartLoc());
    Calendar calendar = Calendar.getInstance(timeZone);
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
    CandidateCollector candidateCollector = new CandidateCollector(request, response, calendar);
    candidateCollector.collectCandidate();


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

}
