package com.appspot.planner.model;

import java.util.ArrayList;

public class Message {
  static public class Event {
    public String content;
    public enum Type {
      FOOD,
      MOVIE,
      TRANSPORT
    }
    public Type type;
  }

  static public class Spec {
    public long startTime;
    public long endTime;
    public String length;
    public String startLoc;
    public String endLoc;
    public int price;
    public int numberOfPeople;
    public enum TravelMode {
      DRIVING,
      WALKING,
      BICYCLING,
      TRANSIT
    }
    public TravelMode travelMode;
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