package com.appspot.planner.model;

import java.util.ArrayList;

public class Transit {
  public String status;
  public Duration duration;
  static public class Distance {
    public int value;
    public String text;
  }
  public Distance distance;
}
