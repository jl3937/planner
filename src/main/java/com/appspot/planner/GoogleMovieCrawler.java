package com.appspot.planner;

import com.appspot.planner.model.Movie;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class GoogleMovieCrawler {
  private static final String GOOGLE_MOVIE_URL = "http://www.google.com/movies";
  public GoogleMovieCrawler() {}
  public ArrayList<Movie> searchMovie(String name, String loc) {
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
    Elements movieEls = doc.getElementsByClass("movie");
    ArrayList<Movie> movies = new ArrayList<Movie>();
    for (Element movieEl : movieEls) {
      Movie movie = new Movie();
      movie.length =
          movieEl.getElementsByClass("info").get(1).html().split(" - ")[0];
      Elements theaterEls = movieEl.getElementsByClass("theater");
      for (Element theaterEl : theaterEls) {
        Movie.Theater theater = new Movie.Theater();
        theater.name =
            theaterEl.getElementsByClass("name").first().child(0).html();
        theater.address =
            theaterEl.getElementsByClass("address").first().html();
        Element timesEl = theaterEl.getElementsByClass("times").first();
        int amIndex = -1, pmIndex = -1;
        int i = 0;
        for (Element timeEl : timesEl.children()) {
          String time = timeEl.ownText();
          if (time.endsWith("am")) {
            amIndex = i;
          } else if (time.endsWith("pm")) {
            pmIndex = i;
          }
          theater.times.add(time);
          ++i;
        }
        movie.theaters.add(theater);
      }
      movies.add(movie);
    }
    
    return movies;
  }
}
