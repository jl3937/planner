package com.appspot.planner.model;

import java.util.List;

public class DistanceMatrixResult {
  public String status;
  public List<String> originAddresses;
  public List<String> destinationAddresses;

  static public class Row {
    public List<Transit> elements;
  }

  public List<Row> rows;
}
