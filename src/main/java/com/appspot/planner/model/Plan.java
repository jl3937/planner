package com.appspot.planner.model;

import java.util.ArrayList;
import java.util.List;

public class Plan {
  public Plan() {
    schedule = new ArrayList<>();
  }

  public List<TimeSlot> schedule;
}
