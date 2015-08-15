package com.appspot.planner.model;

import java.util.ArrayList;

public class Movie {
  public String length;
  static public class Theater {
    public String name;
    public String address;
    public ArrayList<String> times;
    public Theater() {
      times = new ArrayList<String>();
    }
  }
  public ArrayList<Theater> theaters;
  public Movie() {
    theaters = new ArrayList<Theater>();
  }
}
