package com.appspot.planner.util;


import com.appspot.planner.GoogleGeoAPI;
import com.appspot.planner.proto.PlannerProtos.Location;
import com.appspot.planner.proto.PlannerProtos.Requirement;
import com.appspot.planner.proto.PlannerProtos.Time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Util {
  private static final double EARTH_RADIUS = 6378137;
  // meters per millisecond
  private static final double DRIVING_VELOCITY = 72 / 3600.0;  // 72km/h
  private static final double WALKING_VELOCITY = 5 / 3600.0;  // 5km/h
  private static final double BICYCLING_VELOCITY = 15 / 3600.0;  // 15km/h
  private static final double TRANSIT_VELOCITY = 36 / 3600.0;  // 36km/h

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

  public static long getEstimatedDuration(Location location1, Location location2, Requirement.TravelMode travelMode) {
    double distance = getDistance(location1, location2) * 1.414;
    switch (travelMode) {
      case DRIVING:
        return (long) (distance / DRIVING_VELOCITY);
      case WALKING:
        return (long) (distance / WALKING_VELOCITY);
      case BICYCLING:
        return (long) (distance / BICYCLING_VELOCITY);
      case TRANSIT:
        return (long) (distance / TRANSIT_VELOCITY);
    }
    return (long) (distance / DRIVING_VELOCITY);
  }

  public static Time getTimeFromTimestamp(long timestamp, Calendar calendar) {
    Time.Builder time = Time.newBuilder();
    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mmaa");
    dateFormat.setTimeZone(calendar.getTimeZone());
    Date date = new Date();
    date.setTime(timestamp);
    time.setValue(timestamp);
    time.setText(dateFormat.format(date));
    return time.build();
  }

  public static Time getTimeByHourMinute(int hourMinute, long curTime, int curHourMinute, Calendar calendar) {
    long time = curTime + TimeUnit.MILLISECONDS.convert((hourMinute / 100 - curHourMinute / 100), TimeUnit.HOURS) +
        TimeUnit.MILLISECONDS.convert((hourMinute % 100 - curHourMinute % 100), TimeUnit.MINUTES);
    // hourMinute indicates the second day.
    if (hourMinute < curHourMinute) {
      time += TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
    }
    return Util.getTimeFromTimestamp(time, calendar);
  }
}