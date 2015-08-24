package com.appspot.planner;

import com.appspot.planner.model.Movie;
import com.appspot.planner.model.Time;
import com.appspot.planner.util.UrlFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMovieCrawler {
  private static final String GOOGLE_MOVIE_URL = "http://www.google.com/movies";

  public GoogleMovieCrawler() {
  }

  public ArrayList<Movie> searchMovie(String name, String loc) {
    UrlFetcher urlFetcher = new UrlFetcher(GOOGLE_MOVIE_URL);
    urlFetcher.addParameter("q", name);
    urlFetcher.addParameter("near", loc);
    long baseTimestamp = getBaseTimestamp();
    String result = urlFetcher.getResult();
    Document doc = Jsoup.parse(result);
    Elements movieEls = doc.getElementsByClass("movie");
    ArrayList<Movie> movies = new ArrayList<>();
    for (Element movieEl : movieEls) {
      Movie movie = new Movie();
      movie.image = "http:" + movieEl.getElementsByClass("img").first().getElementsByTag("img").first().attr("src");
      movie.name = movieEl.getElementsByClass("desc").first().getElementsByTag("h2").first().ownText();
      Elements linkEls = movieEl.getElementsByClass("info").get(0).getElementsByTag("a");
      for (Element linkEl : linkEls) {
        if (linkEl.ownText().equals("Trailer")) {
          String url = linkEl.attr("href");
          int begin = url.indexOf("q=") + 2;
          int end = url.indexOf("&");
          if (begin >= 2 && end > begin) {
            try {
              movie.trailer = URLDecoder.decode(url.substring(begin, end), "UTF-8");
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
          }
          break;
        }
      }
      Element infoEl = movieEl.getElementsByClass("info").get(1);
      movie.info = infoEl.ownText();
      int directorIndex = movie.info.indexOf("Director");
      if (directorIndex > 0) {
        movie.info = movie.info.substring(0, directorIndex - 1);
      }
      movie.duration.text = movie.info.split(" - ")[0];
      movie.duration.value = parseDuration(movie.duration.text);
      Elements spanEls = infoEl.getElementsByTag("span");
      for (Element spanEl : spanEls) {
        if (spanEl.attr("itemprop").equals("director")) {
          movie.director = spanEl.ownText();
        } else if (spanEl.attr("itemprop").equals("actors")) {
          movie.actors.add(spanEl.ownText());
        }
      }
      movie.desc = movieEl.getElementsByClass("syn").first().getElementsByTag("span").first().ownText();
      Elements theaterEls = movieEl.getElementsByClass("theater");
      for (Element theaterEl : theaterEls) {
        Movie.Theater theater = new Movie.Theater();
        theater.name = theaterEl.getElementsByClass("name").first().child(0).html();
        theater.address = theaterEl.getElementsByClass("address").first().ownText();
        Element timesEl = theaterEl.getElementsByClass("times").first();
        int amIndex = -1, pmIndex = -1;
        int index = 0;
        for (Element timeEl : timesEl.children()) {
          Element linkEl = timeEl.getElementsByTag("a").first();
          if (linkEl == null) {
            continue;
          }
          Time time = new Time();
          time.text = linkEl.ownText();
          if (time.text.endsWith("am")) {
            amIndex = index;
          } else if (time.text.endsWith("pm")) {
            pmIndex = index;
          }
          theater.times.add(time);
          ++index;
        }
        for (int i = 0; i < amIndex; ++i) {
          Time time = theater.times.get(i);
          time.text += "am";
        }
        for (int i = amIndex + 1; i < pmIndex; ++i) {
          Time time = theater.times.get(i);
          time.text += "pm";
        }
        for (int i = 0; i < theater.times.size(); ++i) {
          Time time = theater.times.get(i);
          time.value = parseTime(time.text, baseTimestamp);
        }
        movie.theaters.add(theater);
      }
      movies.add(movie);
    }

    return movies;
  }

  private long parseDuration(String text) {
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

  private long getBaseTimestamp() {
    Calendar calendar = Calendar.getInstance();
    Date date = calendar.getTime();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
    TimeZone timeZone = TimeZone.getTimeZone("PST");
    dateFormat.setTimeZone(timeZone);
    String currentTime = dateFormat.format(date);

    Pattern pattern = Pattern.compile("(\\d+)-(\\d+)-(\\d+)T(\\d+):(\\d+):" + "(\\d+)(\\w+)");
    Matcher matcher = pattern.matcher(currentTime);
    if (matcher.find() && matcher.groupCount() == 7) {
      int year = Integer.parseInt(matcher.group(1));
      int month = Integer.parseInt(matcher.group(2));
      int day = Integer.parseInt(matcher.group(3));
      String zone = matcher.group(7);
      try {
        String baseTime = String.format("%04d-%02d-%02dT00:00:00%s", year, month, day, zone);
        date = dateFormat.parse(baseTime);
        return date.getTime();
      } catch (ParseException e) {
      }
    }
    return 0;
  }

  private long parseTime(String text, long baseTimestamp) {
    long milliseconds = baseTimestamp;
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
      milliseconds += TimeUnit.MILLISECONDS.convert(hour, TimeUnit.HOURS);
      milliseconds += TimeUnit.MILLISECONDS.convert(minute, TimeUnit.MINUTES);
    }
    return milliseconds;
  }
}
