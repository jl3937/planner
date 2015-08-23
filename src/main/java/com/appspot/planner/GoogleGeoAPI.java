package com.appspot.planner;

import com.appspot.planner.model.DistanceMatrixResult;
import com.appspot.planner.model.PlaceDetailResult;
import com.appspot.planner.model.PlaceResult;
import com.appspot.planner.util.UrlFetcher;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GoogleGeoAPI {
  private static final String DISTANCE_MATRIX_API_URL = "https://maps" + ".googleapis.com/maps/api/distancematrix/json";
  private static final String PLACE_API_URL = "https://maps.googleapis" + ".com/maps/api/place/textsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis" + ".com/maps/api/place/details/json";

  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";

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
    DistanceMatrixResult result = new DistanceMatrixResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }

  public PlaceResult searchPlace(String keyword, String location) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_API_URL);
    urlFetcher.addParameter("query", keyword + " near " + location);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    PlaceResult result = new PlaceResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }

  public PlaceDetailResult getPlaceDetail(String placeid) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_DETAIL_API_URL);
    urlFetcher.addParameter("placeid", placeid);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    PlaceDetailResult result = new PlaceDetailResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }
}
