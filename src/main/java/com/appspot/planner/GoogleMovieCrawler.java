package com.appspot.planner;

import com.appspot.planner.model.Movie;
import com.appspot.planner.util.UrlFetcher;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class GoogleMovieCrawler {
  private Pattern durationPattern = Pattern.compile("(\\d+)hr\\s+(\\d+)min");
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
      movie.duration.text =
        movieEl.getElementsByClass("info").get(1).html().split(" - ")[0];
      movie.duration.value = parseDuration(movie.duration.text);
      Elements theaterEls = movieEl.getElementsByClass("theater");
      for (Element theaterEl : theaterEls) {
        Movie.Theater theater = new Movie.Theater();
        theater.name =
          theaterEl.getElementsByClass("name").first().child(0).html();
        theater.address =
          theaterEl.getElementsByClass("address").first().ownText();
        Element timesEl = theaterEl.getElementsByClass("times").first();
        int amIndex = -1, pmIndex = -1;
        int index = 0;
        for (Element timeEl : timesEl.children()) {
          String time = "";
          Element linkEl = timeEl.getElementsByTag("a").first();
          if (linkEl != null) {
            time = linkEl.ownText();
          } else {
            time = timeEl.ownText();
          }
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

  private long parseDuration(String text) {
    long milliseconds = 0;
    Matcher matcher = durationPattern.matcher(text);
    if (matcher.find() && matcher.groupCount() == 2) {
      int hours = Integer.parseInt(matcher.group(1));
      milliseconds += TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS);
      int minutes = Integer.parseInt(matcher.group(2));
      milliseconds += TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
    }
    return milliseconds;
  }
}
