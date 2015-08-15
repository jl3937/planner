package com.appspot.planner.model;

import java.util.ArrayList;

public class DistanceMatrixResult {
  public String status;
  public ArrayList<String> originAddresses;
  public ArrayList<String> destinationAddresses;
  static public class Row {
    static public class Element {
      public String status;
      static public class Duration {
        public int value;
        public String text;
      }
      public Duration duration;
      static public class Distance {
        public int value;
        public String text;
      }
      public Distance distance;
    }
    public ArrayList<Element> elements;
  }
  public ArrayList<Row> rows;
}
