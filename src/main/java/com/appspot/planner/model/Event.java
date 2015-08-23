package com.appspot.planner.model;

import java.util.List;

public class Event {
  public String content;

  public enum Type {
    FOOD,
    MOVIE,
    TRANSPORT
  }

  public Type type;
  public Spec requirement;
  public List<String> avoid;
}
