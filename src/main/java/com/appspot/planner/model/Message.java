package com.appspot.planner.model;

import java.util.ArrayList;
import java.util.List;

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
    public PlaceDetailResult placeDetailResult;
  }

  public Spec requirement;
  public List<Event> events;
  public List<TimeSlot> schedule;

  public Message() {
    events = new ArrayList<>();
    schedule = new ArrayList<>();
  };
}
