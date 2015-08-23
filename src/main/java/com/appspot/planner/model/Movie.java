package com.appspot.planner.model;

import java.util.ArrayList;
import java.util.List;

public class Movie {
  public String image;
  public String name;
  public String info;
  public String desc;
  public Duration duration;
  public String director;
  public List<String> actors;

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
    actors = new ArrayList<>();
    duration = new Duration();
    theaters = new ArrayList<>();
  }
}
