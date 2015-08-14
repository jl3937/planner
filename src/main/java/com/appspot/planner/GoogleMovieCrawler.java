package com.appspot.planner;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GoogleMovieCrawler {
  private static final String GOOGLE_MOVIE_URL = "http://www.google.com/movies";
  public GoogleMovieCrawler() {}
  public Document searchMovie(String name, String loc) {
    String result = "";
    try {
      URL url = new URL(GOOGLE_MOVIE_URL + "?q=" + URLEncoder.encode(name) + "&near=" + URLEncoder.encode(loc));
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
    Document doc = Jsoup.parse(result);
    return doc;
  }
}
