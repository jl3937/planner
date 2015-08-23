package com.appspot.planner.model;

import java.util.List;

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

    public List<Period> periods;
    public List<String> weekdayText;
  }

  public OpeningHours openingHours;

  static public class Review {
    static public class Aspect {
      public int rating;
      public String type;
    }

    public List<Aspect> aspects;
    public String authorName;
    public String authorUrl;
    public String language;
    public int rating;
    public String text;
    public long time;
  }

  public List<Review> reviews;
  public List<String> types;
  public String url;
  public int userRatingsTotal;
  public String website;
}
