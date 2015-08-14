package com.appspot.planner;

import java.util.ArrayList;

public class Message {
  static public class Event {
    public String content;
    enum Type {
      FOOD,
      MOVIE
    }
    public Type type;

    public Event() {
      type = Type.FOOD;
    }
  }

  static public class Spec {
    public long startTime;
    public long endTime;
    public String startLoc;
    public String endLoc;
    public int price;
    public int numberOfPeople;
  }
  
  static public class TimeSlot {
    public TimeSlot() {
      event = new Event();
      spec = new Spec();
    }
    public Event event;
    public Spec spec;
  }

  public Spec requirement;
  public ArrayList<Event> events;
  public ArrayList<TimeSlot> schedule;

  public Message() {
    events = new ArrayList<Event>();
    schedule = new ArrayList<TimeSlot>();
  };
}
