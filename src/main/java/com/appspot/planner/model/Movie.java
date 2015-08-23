package com.appspot.planner.model;

import java.util.ArrayList;
import java.util.List;

public class Movie {
  public Duration duration;
  static public class Theater {
    public String name;
    public String address;
    public List<Time> times;
    public Theater() {
      times = new ArrayList<>();
    }
  }
  public List<Theater> theaters;
  public Theater theater;
  public Movie() {
    duration = new Duration();
    theaters = new ArrayList<>();
  }
}
