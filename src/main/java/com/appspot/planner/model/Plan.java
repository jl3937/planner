package com.appspot.planner.model;

import java.util.ArrayList;

public class Plan {
  public Plan() {
    schedule = new ArrayList<>();
  }

  public ArrayList<TimeSlot> schedule;
}
