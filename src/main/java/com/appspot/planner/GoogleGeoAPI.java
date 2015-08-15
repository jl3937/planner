package com.appspot.planner;

import com.appspot.planner.model.DistanceMatrixResult;
import com.appspot.planner.model.PlaceDetailResult;
import com.appspot.planner.model.PlaceResult;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class GoogleGeoAPI {
  private static final String DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
  private static final String PLACE_API_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis.com/maps/api/place/details/json";

  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";

  private Gson gson;

  public GoogleGeoAPI() {
    this.gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  }

  public DistanceMatrixResult getDuration(String origin,
                                          String destination,
                                          String mode) {
    String json = "";
    try {
      URL url = new URL(DISTANCE_MATRIX_API_URL +
          "?origins=" + URLEncoder.encode(origin) +
          "&destinations=" + URLEncoder.encode(destination) +
          "&mode=" + mode +
          "&key=" + API_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line = "";
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        json += line + "\n";
      }
      reader.close();
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    DistanceMatrixResult result = new DistanceMatrixResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }

  public PlaceResult searchPlace(String keyword, String location) {
    String json = "";
    try {
      URL url = new URL(PLACE_API_URL +
          "?query=" + URLEncoder.encode(keyword) + "+near+" + URLEncoder.encode(location) +
          "&key=" + API_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line = "";
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        json += line + "\n";
      }
      reader.close();
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    PlaceResult result = new PlaceResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }

  public PlaceDetailResult getPlaceDetail(String placeId) {
    String json = "";
    try {
      URL url = new URL(PLACE_DETAIL_API_URL + "?placeid=" + placeId + "&key=" + API_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line = "";
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        json += line + "\n";
      }
      reader.close();
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    PlaceDetailResult result = new PlaceDetailResult();
    result = this.gson.fromJson(json, result.getClass());
    return result;
  }
}
