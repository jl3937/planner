package com.appspot.planner;

import java.util.Arrays;

public class Message {
  static public class Event {
    public String content;
    public int type;
  }

  static public class Spec {
    public long startTime;
    public long endTime;
    public String startLoc;
    public String EndLoc;
    public int price;
    public int numberOfPeople;
  }
  
  static public class Slot {
    public Event event;
    public Spec spec;
  }

  public Spec requirement;
  public Event[] events;
  public Slot[] schedule;

  public Message() {};
}
