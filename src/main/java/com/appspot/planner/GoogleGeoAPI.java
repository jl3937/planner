package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.UrlFetcher;
import com.googlecode.protobuf.format.JsonFormat;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GoogleGeoAPI {
  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";
  private static final String DISTANCE_MATRIX_API_URL = "https://maps" + ".googleapis.com/maps/api/distancematrix/json";
  private static final String GEOCODE_API_URL = "https://maps" + ".googleapis.com/maps/api/geocode/json";
  private static final String PLACE_SEARCH_API_URL = "https://maps.googleapis" + "" +
      ".com/maps/api/place/radarsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis" + ".com/maps/api/place/details/json";
  private static final String TIMEZONE_API_URL = "https://maps.googleapis.com/maps/api/timezone/json";

  static public TimeZone getTimeZone(Location location) {
    location = getLocation(location);
    UrlFetcher urlFetcher = new UrlFetcher(TIMEZONE_API_URL);
    urlFetcher.addParameter("location", location.getCoordinate().getLat() + "," + location.getCoordinate().getLng());
    urlFetcher.addParameter("timestamp", Long.toString(Calendar.getInstance().getTimeInMillis() / 1000));
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    TimeZoneResult.Builder builder = TimeZoneResult.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      TimeZoneResult result = builder.build();
      return TimeZone.getTimeZone(result.getTimeZoneId());
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  static public long getDuration(Location origin, Location destination, String mode) {
    UrlFetcher urlFetcher = new UrlFetcher(DISTANCE_MATRIX_API_URL);
    urlFetcher.addParameter("origins", origin.getAddress());
    urlFetcher.addParameter("destinations", destination.getAddress());
    urlFetcher.addParameter("mode", mode);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    DistanceMatrixResult.Builder builder = DistanceMatrixResult.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      DistanceMatrixResult result = builder.build();
      long minDuration = -1;
      for (Transit transit : result.getRows(0).getElementsList()) {
        if (minDuration == -1 || transit.getDuration().getValue() < minDuration) {
          minDuration = transit.getDuration().getValue();
        }
      }
      return TimeUnit.MILLISECONDS.convert(minDuration, TimeUnit.SECONDS);
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return 0;
  }

  static public Location getLocation(Location location) {
    if (location.hasCoordinate() || !location.hasAddress()) {
      return location;
    }
    UrlFetcher urlFetcher = new UrlFetcher(GEOCODE_API_URL);
    urlFetcher.addParameter("address", location.getAddress());
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    GeocodeResults.Builder builder = GeocodeResults.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      GeocodeResults results = builder.build();
      if (results.getResultsCount() == 0) {
        return null;
      }
      return Location.newBuilder().mergeFrom(location).setCoordinate(results.getResults(0).getGeometry().getLocation
          ()).build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  static public PlaceResult searchPlace(String keyword, Location location, int radius, Event.Type type) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_SEARCH_API_URL);
    urlFetcher.addParameter("location", location.getCoordinate().getLat() + "," + location.getCoordinate().getLng());
    urlFetcher.addParameter("radius", String.valueOf(radius));
    urlFetcher.addParameter("keyword", keyword);
    if (type == Event.Type.FOOD) {
      urlFetcher.addParameter("types", "food|cafe");
    }
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    PlaceResult.Builder builder = PlaceResult.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      return builder.build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  static public PlaceDetailResult getPlaceDetail(String placeid) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_DETAIL_API_URL);
    urlFetcher.addParameter("placeid", placeid);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    PlaceDetailResult.Builder builder = PlaceDetailResult.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      return builder.build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }
}
