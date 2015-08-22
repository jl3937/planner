package com.appspot.planner.model;

import java.util.ArrayList;

public class Spec {
  // Time in milliseconds.
  public long startTime;
  public long endTime;

  public String startLoc;
  public String endLoc;

  public int priceLevel;
  public int numberOfPeople;
  public enum TravelMode {
    DRIVING,
    WALKING,
    BICYCLING,
    TRANSIT
  }
  public TravelMode travelMode;
}
