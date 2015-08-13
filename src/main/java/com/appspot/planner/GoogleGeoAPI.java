package com.appspot.planner;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GoogleGeoAPI {
  private static final String DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
  private static final String PLACE_API_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";
  private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis.com/maps/api/place/details/json";

  private static final String API_KEY = "AIzaSyAD6lYA1_7oUGMAPkRJ3duNIlPRuzohvNw";

  public GoogleGeoAPI() {
  }

  public JSONObject getDuration(String origin, String destination) {
    String result = "";
    try {
      URL url = new URL(DISTANCE_MATRIX_API_URL + "?origins=" + URLEncoder.encode(origin) + "&destinations=" + URLEncoder.encode(destination) + "&key=" + API_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line = "";
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        result += line + "\n";
      }
      reader.close();
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    JSONParser parser = new JSONParser();
    try {
      return (JSONObject) parser.parse(result);
    } catch (ParseException pe) {
      return null;
    }
  }

  public JSONObject searchPlace(String keyword, String location) {
    String result = "";
    try {
      URL url = new URL(PLACE_API_URL + "?query=" + URLEncoder.encode(keyword) + "+near+" + URLEncoder.encode(location) + "&key=" + API_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line = "";
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        result += line + "\n";
      }
      reader.close();
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    JSONParser parser = new JSONParser();
    try {
      return (JSONObject) parser.parse(result);
    } catch (ParseException pe) {
      return null;
    }

  }
}
