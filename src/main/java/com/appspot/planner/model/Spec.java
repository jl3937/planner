package com.appspot.planner.model;

public class Spec {
  // Time in milliseconds.
  static public class Time {
    public long value;
    public String text;
  }

  public Time startTime;
  public Time endTime;

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

  public Spec() {
    startTime = new Time();
    endTime = new Time();
  }
}
