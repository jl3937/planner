package com.appspot.planner.model;

import java.util.ArrayList;

public class DistanceMatrixResult {
  public String status;
  public ArrayList<String> originAddresses;
  public ArrayList<String> destinationAddresses;
  static public class Row {
    public ArrayList<Transit> elements;
  }
  public ArrayList<Row> rows;
}
