package com.appspot.planner.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

public class UrlFetcher {
  String url;
  ArrayList<String> parameters;
  public UrlFetcher(String url) {
    this.url = url;
    parameters = new ArrayList<String>();
  }
  public void addParameter(String key, String value) {
    String parameter = key + "=" + URLEncoder.encode(value);
    this.parameters.add(parameter);
  }
  public String getResult() {
    String result = "";
    try {
      URL url = new URL(this.url + "?" + String.join("&", this.parameters));
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(url.openStream()));
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
    return result;
  }
}
