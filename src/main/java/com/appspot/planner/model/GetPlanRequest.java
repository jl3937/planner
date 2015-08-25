package com.appspot.planner.model;

import java.util.List;

public class GetPlanRequest {
  public Spec requirement;
  public List<Event> events;
  public String timeZone;
}
