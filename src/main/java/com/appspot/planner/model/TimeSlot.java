package com.appspot.planner.model;

import java.util.ArrayList;

public class TimeSlot {
  public TimeSlot() {
    event = new Event();
    spec = new Spec();
  }

  public Event event;
  public Spec spec;

  // Only one of below is set for each time slot.
  public Movie movie;
  public Place place;
  public Transit transit;
}
