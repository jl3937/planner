package com.appspot.planner;

import com.appspot.planner.model.Movie;
import com.appspot.planner.util.UrlFetcher;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class GoogleMovieCrawler {
  private static final String GOOGLE_MOVIE_URL = "http://www.google.com/movies";
  public GoogleMovieCrawler() {}
  public ArrayList<Movie> searchMovie(String name, String loc) {
    UrlFetcher urlFetcher = new UrlFetcher(GOOGLE_MOVIE_URL);
    urlFetcher.addParameter("q", name);
    urlFetcher.addParameter("near", loc);
    String result = urlFetcher.getResult();
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
        int index = 0;
        for (Element timeEl : timesEl.children()) {
          String time = timeEl.ownText();
          if (time.endsWith("am")) {
            amIndex = index;
          } else if (time.endsWith("pm")) {
            pmIndex = index;
          }
          theater.times.add(time);
          ++index;
        }
        for (int i = 0; i < amIndex; ++i) {
          theater.times.set(i, theater.times.get(i) + "am");
        }
        for (int i = amIndex + 1; i < pmIndex; ++i) {
          theater.times.set(i, theater.times.get(i) + "pm");
        }
        movie.theaters.add(theater);
      }
      movies.add(movie);
    }

    return movies;
  }
}
