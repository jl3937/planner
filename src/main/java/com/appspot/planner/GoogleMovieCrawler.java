package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.UrlFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMovieCrawler {
  private static final String GOOGLE_MOVIE_URL = "http://www.google.com/movies";

  static public ArrayList<Movie> searchMovie(String name, Location loc, int date, Calendar calendar) {
    UrlFetcher urlFetcher = new UrlFetcher(GOOGLE_MOVIE_URL);
    urlFetcher.addParameter("q", name);
    urlFetcher.addParameter("near", loc.getAddress());
    urlFetcher.addParameter("date", String.valueOf(date));
    String result = urlFetcher.getResult();
    Document doc = Jsoup.parse(result);
    Elements movieEls = doc.getElementsByClass("movie");
    ArrayList<Movie> movies = new ArrayList<>();
    for (Element movieEl : movieEls) {
      Movie.Builder movie = Movie.newBuilder();
      movie.setImage("http:" + movieEl.getElementsByClass("img").first().getElementsByTag("img").first().attr("src"));
      movie.setName(movieEl.getElementsByClass("desc").first().getElementsByTag("h2").first().ownText());
      Elements linkEls = movieEl.getElementsByClass("info").get(0).getElementsByTag("a");
      for (Element linkEl : linkEls) {
        if (linkEl.ownText().equals("Trailer")) {
          String url = linkEl.attr("href");
          int begin = url.indexOf("q=") + 2;
          int end = url.indexOf("&");
          if (begin >= 2 && end > begin) {
            try {
              movie.setTrailer(URLDecoder.decode(url.substring(begin, end), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
          }
          break;
        }
      }
      Element infoEl = movieEl.getElementsByClass("info").get(1);
      String info = infoEl.ownText();
      int directorIndex = info.indexOf("Director");
      if (directorIndex > 0) {
        movie.setInfo(info.substring(0, directorIndex - 1));
      }
      String durationText = info.split(" - ")[0];
      movie.setDuration(Duration.newBuilder().setText(durationText).setValue(parseDuration(durationText)));
      Elements spanEls = infoEl.getElementsByTag("span");
      for (Element spanEl : spanEls) {
        if (spanEl.attr("itemprop").equals("director")) {
          movie.setDirector(spanEl.ownText());
        } else if (spanEl.attr("itemprop").equals("actors")) {
          movie.addActors(spanEl.ownText());
        }
      }
      movie.setDesc(movieEl.getElementsByClass("syn").first().getElementsByTag("span").first().ownText());
      Elements theaterEls = movieEl.getElementsByClass("theater");
      for (Element theaterEl : theaterEls) {
        Theater.Builder theater = Theater.newBuilder();
        theater.setName(theaterEl.getElementsByClass("name").first().child(0).html());
        theater.setAddress(theaterEl.getElementsByClass("address").first().ownText());
        Element timesEl = theaterEl.getElementsByClass("times").first();
        int amIndex = -1, pmIndex = -1;
        int index = 0;
        for (Element timeEl : timesEl.children()) {
          Element linkEl = timeEl.getElementsByTag("a").first();
          if (linkEl == null) {
            continue;
          }
          Time.Builder time = Time.newBuilder();
          time.setText(linkEl.ownText());
          if (linkEl.ownText().endsWith("am")) {
            amIndex = index;
          } else if (linkEl.ownText().endsWith("pm")) {
            pmIndex = index;
          }
          theater.addTimes(time);
          ++index;
        }
        for (int i = 0; i < amIndex; ++i) {
          theater.getTimesBuilder(i).setText(theater.getTimes(i).getText() + "am");
        }
        for (int i = amIndex + 1; i < pmIndex; ++i) {
          theater.getTimesBuilder(i).setText(theater.getTimes(i).getText() + "pm");
        }
        for (int i = 0; i < theater.getTimesCount(); ++i) {
          theater.getTimesBuilder(i).setValue(parseTime(theater.getTimes(i).getText(), calendar));
        }
        movie.addTheaters(theater.build());
      }
      movies.add(movie.build());
    }

    return movies;
  }

  static private long parseDuration(String text) {
    long milliseconds = 0;
    Pattern pattern = Pattern.compile("(\\d+)hr\\s+(\\d+)min");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find() && matcher.groupCount() == 2) {
      int hours = Integer.parseInt(matcher.group(1));
      milliseconds += TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS);
      int minutes = Integer.parseInt(matcher.group(2));
      milliseconds += TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
    }
    return milliseconds;
  }

  static private long parseTime(String text, Calendar calendar) {
    long originalTime = calendar.getTimeInMillis();
    Pattern pattern = Pattern.compile("(\\d+):(\\d+)(\\w+)");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find() && matcher.groupCount() == 3) {
      int hour = Integer.parseInt(matcher.group(1));
      int minute = Integer.parseInt(matcher.group(2));
      String marker = matcher.group(3);
      if (hour == 12 && marker.equals("am")) {
        hour = 0;
      }
      if (hour < 12 && marker.equals("pm")) {
        hour += 12;
      }
      calendar.set(Calendar.HOUR_OF_DAY, hour);
      calendar.set(Calendar.MINUTE, minute);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      long time = calendar.getTimeInMillis();
      calendar.setTimeInMillis(originalTime);
      return time;
    }
    return 0;
  }
}
