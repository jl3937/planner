package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.UrlFetcher;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.protobuf.format.JsonFormat;

public class GoogleGeoAPI {
  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";
  private static final String DISTANCE_MATRIX_API_URL = "https://maps" + ".googleapis.com/maps/api/distancematrix/json";
  private static final String PLACE_API_URL = "https://maps.googleapis" + ".com/maps/api/place/textsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis" + ".com/maps/api/place/details/json";

  private Gson gson;

  public GoogleGeoAPI() {
    this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  }

  public DistanceMatrixResult getDuration(String origin, String destination, String mode) {
    UrlFetcher urlFetcher = new UrlFetcher(DISTANCE_MATRIX_API_URL);
    urlFetcher.addParameter("origins", origin);
    urlFetcher.addParameter("destinations", destination);
    urlFetcher.addParameter("mode", mode);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    DistanceMatrixResult.Builder builder = DistanceMatrixResult.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      return builder.build();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  public PlaceResult searchPlace(String keyword, String location) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_API_URL);
    urlFetcher.addParameter("query", keyword + " near " + location);
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

  public PlaceDetailResult getPlaceDetail(String placeid) {
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
