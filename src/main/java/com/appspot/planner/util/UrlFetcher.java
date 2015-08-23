package com.appspot.planner.util;

import com.google.common.base.Joiner;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UrlFetcher {
  private String url;
  private List<String> parameters;

  public UrlFetcher(String url) {
    this.url = url;
    parameters = new ArrayList<>();
  }

  public void addParameter(String key, String value) {
    String parameter = key + "=" + URLEncoder.encode(value);
    this.parameters.add(parameter);
  }
  public String getResult() {
    String result = "";
    try {
      URL url = new URL(this.url + "?" + Joiner.on("&").join(this.parameters));
      System.out.println(url.toString());
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
      System.out.println(result);
    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }
    return result;
  }
}
