package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.UrlFetcher;
import com.googlecode.protobuf.format.JsonFormat;

public class GoogleGeoAPI {
  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";
  private static final String DISTANCE_MATRIX_API_URL = "https://maps" + ".googleapis.com/maps/api/distancematrix/json";
  private static final String GEOCODE_API_URL = "https://maps" + ".googleapis.com/maps/api/geocode/json";
  private static final String PLACE_SEARCH_API_URL = "https://maps.googleapis" + "" +
      ".com/maps/api/place/radarsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis" + ".com/maps/api/place/details/json";

  public GoogleGeoAPI() {
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

  public Geometry.Location getLocation(String address) {
        UrlFetcher urlFetcher = new UrlFetcher(GEOCODE_API_URL);
    urlFetcher.addParameter("address", address);
    urlFetcher.addParameter("key", API_KEY);
    String json = urlFetcher.getResult();
    GeocodeResults.Builder builder = GeocodeResults.newBuilder();
    try {
      JsonFormat.merge(json, builder);
      GeocodeResults results = builder.build();
      if (results.getResultsCount() == 0) {
        return null;
      }
      return results.getResults(0).getGeometry().getLocation();
    } catch (JsonFormat.ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  public PlaceResult searchPlace(String keyword, Geometry.Location location, int radius, Event.Type type) {
    UrlFetcher urlFetcher = new UrlFetcher(PLACE_SEARCH_API_URL);
    urlFetcher.addParameter("location", location.getLat() + "," + location.getLng());
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
