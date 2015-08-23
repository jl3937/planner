package com.appspot.planner.model;

public class Transit {
  public String status;
  public Duration duration;

  static public class Distance {
    public int value;
    public String text;
  }

  public Distance distance;
}
