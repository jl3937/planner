package com.appspot.planner.model;

import java.util.ArrayList;

public class Place {
  public String formattedAddress;
  public String formattedPhoneNumber;
  public static class Geometry {
    public static class Location {
      public String lat;
      public String lng;
    }
    public Location location;
  }
  public Geometry geometry;
  public String icon;
  public String internationalPhoneNumber;
  public String name;
  public int priceLevel;
  public double rating;
  static public class OpeningHours {
    public boolean openNow;
    static public class Period {
      static public class DayTime {
        public int day;
        public String time;
      }
      public DayTime close;
      public DayTime open;
    }
    public ArrayList<Period> periods;
    public ArrayList<String> weekdayText;
  }
  public OpeningHours openingHours;
  static public class Review {
    static public class Aspect {
      public int rating;
      public String type;
    }
    public ArrayList<Aspect> aspects;
    public String authorName;
    public String authorUrl;
    public String language;
    public int rating;
    public String text;
    public long time;
  }
  public ArrayList<Review> reviews;
  public ArrayList<String> types;
  public String url;
  public int userRatingsTotal;
  public String website;
}
