package com.appspot.planner.model;

import java.util.ArrayList;

public class PlaceDetailResult {
  static public class Result {
    public String formattedPhoneNumber;
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
    public String url;
    public int userRatingsTotal;
    public String website;
  }
  public Result result;
  public String status;
}
