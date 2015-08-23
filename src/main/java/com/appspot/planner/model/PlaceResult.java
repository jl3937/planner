package com.appspot.planner.model;

import java.util.List;

public class PlaceResult {
  static public class Result {
    public String formattedAddress;

    static public class Geometry {
      static public class Location {
        public String lat;
        public String lng;
      }
    }

    public Geometry geometry;
    public String id;
    public String name;

    static public class OpeningHours {
      public boolean openNow;
      public List<String> weekdayText;
    }

    public OpeningHours openingHours;
    public String placeId;
    public int priceLevel;
    public double rating;
    public List<String> types;
  }

  public List<Result> results;
}
