package com.appspot.planner.model;

import java.util.ArrayList;

public class Movie {
  public Duration duration;
  static public class Theater {
    public String name;
    public String address;
    public ArrayList<String> times;
    public Theater() {
      times = new ArrayList<>();
    }
  }
  public ArrayList<Theater> theaters;
  public Theater theater;
  public Movie() {
    duration = new Duration();
    theaters = new ArrayList<>();
  }
}
