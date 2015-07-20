package com.appspot.planner;

import java.util.Arrays;

public class Message {

  public long startTime;
  public long endTime;
  public String startLoc;
  public String endLoc;
  public String[] events;

  public Message() {};

  public Message(Message message) {
    this.startTime = message.startTime;
    this.endTime = message.endTime;
    this.startLoc = message.startLoc;
    this.endLoc = message.endLoc;
    if (message.events != null) {
      this.events = Arrays.copyOf(message.events, message.events.length);
    }
  };

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public String getStartLoc() {
    return startLoc;
  }

  public void setStartLoc(String startLoc) {
    this.startLoc = startLoc;
  }

  public String getEndLoc() {
    return endLoc;
  }

  public void setEndLoc(String endLoc) {
    this.endLoc = endLoc;
  }
}
