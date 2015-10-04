package com.appspot.planner.util;


import com.appspot.planner.GoogleGeoAPI;
import com.appspot.planner.proto.PlannerProtos;
import com.appspot.planner.proto.PlannerProtos.Location;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Util {
  private static final double EARTH_RADIUS = 6378137;
  private static final double METERS_PER_MILLI = 0.02;

  private static double rad(double d) {
    return d * Math.PI / 180.0;
  }

  // Return in meters
  public static double getDistance(Location location1, Location location2) {
    location1 = GoogleGeoAPI.getLocation(location1);
    location2 = GoogleGeoAPI.getLocation(location2);
    double lat1 = location1.getCoordinate().getLat();
    double lng1 = location1.getCoordinate().getLng();
    double lat2 = location2.getCoordinate().getLat();
    double lng2 = location2.getCoordinate().getLng();
    double radLat1 = rad(lat1);
    double radLat2 = rad(lat2);
    double a = radLat1 - radLat2;
    double b = rad(lng1) - rad(lng2);
    double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math
        .pow(Math.sin(b / 2), 2)));
    s = s * EARTH_RADIUS;
    s = Math.round(s * 10000) / 10000;
    return s;
  }

  public static long getEstimatedDuration(Location location1, Location location2) {
    return (long) (getDistance(location1, location2) * 1.414 / METERS_PER_MILLI);
  }

  public static PlannerProtos.Time getTimeFromTimestamp(long timestamp, Calendar calendar) {
    PlannerProtos.Time.Builder time = PlannerProtos.Time.newBuilder();
    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mmaa");
    dateFormat.setTimeZone(calendar.getTimeZone());
    Date date = new Date();
    date.setTime(timestamp);
    time.setValue(timestamp);
    time.setText(dateFormat.format(date));
    return time.build();
  }

  public static PlannerProtos.Time getTimeByHourMinute(int hourMinute, long curTime, int curHourMinute, Calendar
      calendar) {
    long time = curTime + TimeUnit.MILLISECONDS.convert((hourMinute / 100 - curHourMinute / 100), TimeUnit.HOURS) +
        TimeUnit.MILLISECONDS.convert((hourMinute % 100 - curHourMinute % 100), TimeUnit.MINUTES);
    // hourMinute indicates the second day.
    if (hourMinute < curHourMinute) {
      time += TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
    }
    return Util.getTimeFromTimestamp(time, calendar);
  }
}