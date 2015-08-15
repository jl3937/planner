package com.appspot.planner.model;

import java.util.ArrayList;

public class PlaceResult {
  public ArrayList<String> htmlAttributions;
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
      public ArrayList<String> weekdayText;
    }
    public OpeningHours openingHours;
    public String placeId;
    public int priceLevel;
    public double rating;
    public ArrayList<String> types;
  }
  public ArrayList<Result> results;
}
