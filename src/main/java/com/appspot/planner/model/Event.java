package com.appspot.planner.model;

import java.util.ArrayList;

public class Event {
  public String content;
  public enum Type {
    FOOD,
    MOVIE,
    TRANSPORT
  }
  public Type type;
  public Spec requirement;
  public ArrayList<String> avoid;
}
