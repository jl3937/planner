package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.Util;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Scheduler {
  public static final long DEFAULT_PLACE_DURATION = 600000;  // 10 min
  public static final long MIN_PLACE_DURATION = 300000;  // 5 min
  public static final long DEFAULT_FOOD_DURATION = 3600000;  // 1 hour
  public static final long MIN_FOOD_DURATION = 1800000;  // 30 min
  public static final long MOVIE_LATE_THRESHOLD = 900000;  // 15 min

  GetPlanRequest request;
  GetPlanResponse.Builder response;
  Calendar calendar;
  int eventCount;
  int[] index;  // index of candidate
  int[] size;
  int[] permutation;  // index of event
  Location startLoc;
  Location endLoc;
  long startTimestamp;
  long endTimestamp;

  Scheduler(GetPlanRequest request, GetPlanResponse.Builder response, Calendar calendar) {
    this.request = request;
    this.response = response;
    this.calendar = calendar;
    eventCount = response.getProcessedEventCount();
    index = new int[eventCount];
    size = new int[eventCount];
    permutation = new int[eventCount];
    for (int i = 0; i < eventCount; ++i) {
      index[i] = 0;
      size[i] = response.getProcessedEvent(i).getCandicatesCount();
    }
    startLoc = request.getRequirement().getStartLoc();
    endLoc = request.getRequirement().hasEndLoc() ? request.getRequirement().getStartLoc() : startLoc;
    startLoc = GoogleGeoAPI.getLocation(startLoc);
    endLoc = GoogleGeoAPI.getLocation(endLoc);
    startTimestamp = calendar.getTimeInMillis();
    endTimestamp = request.getRequirement().getTimePeriod().getEndTime().getValue();
  }

  public GetPlanResponse.Builder getSchedule() {
    for (int i = 0; i < eventCount; ++i) {
      if (size[i] == 0) {
        return response;
      }
    }
    do {
      for (int i = 0; i < eventCount; ++i) {
        permutation[i] = i;
      }
      do {
        Schedule schedule = trySchedule(false);
        if (schedule != null) {
          response.addScheduleCandidate(schedule);
          continue;
        }
        schedule = trySchedule(true);
        if (schedule != null) {
          response.addScheduleCandidate(schedule);
        }
      } while (getNextPermutation());
    } while (getNextIndex());
    Schedule best = null;
    for (Schedule schedule : response.getScheduleCandidateList()) {
      if (best == null || schedule.getScore() > best.getScore()) {
        best = schedule;
      }
    }
    response.setSchedule(best);
    return response;
  }

  // Return null if can't schedule
  private Schedule trySchedule(boolean tight) {
    Schedule.Builder schedule = Schedule.newBuilder();
    long time = startTimestamp;
    Location previousLoc = startLoc;
    double ratingSum = 0;
    int ratingCount = 0;
    double priceLevelSum = 0;
    int priceLevelCount = 0;
    for (int i = 0; i < eventCount; ++i) {
      Event event = response.getProcessedEvent(permutation[i]);
      TimeSlot candidate = event.getCandicates(index[permutation[i]]);
      Location eventLoc = candidate.getSpec().getStartLoc();
      // Add transport
      long duration = Util.getEstimatedDuration(previousLoc, eventLoc);
      time = addTimeSlot(time, duration, null, previousLoc, eventLoc, schedule);
      // Add event
      if (time < candidate.getSpec().getTimePeriod().getStartTime().getValue()) {
        time = candidate.getSpec().getTimePeriod().getStartTime().getValue();
      }
      duration = getEventDuration(event.getType(), time, candidate, tight);
      if (duration < 0) {
        return null;
      }
      time = addTimeSlot(time, duration, candidate.getEvent(), eventLoc, eventLoc, schedule);
      previousLoc = eventLoc;
      if (candidate.getSpec().getRating() != 0) {
        ratingSum += candidate.getSpec().getRating();
        ++ratingCount;
      }
      if (candidate.getSpec().getPriceLevel() != 0) {
        priceLevelSum += candidate.getSpec().getPriceLevel();
        ++priceLevelCount;
      }
    }
    long duration = Util.getEstimatedDuration(previousLoc, endLoc);
    time = addTimeSlot(time, duration, null, previousLoc, endLoc, schedule);
    if (endTimestamp > 0 && time > endTimestamp) {
      return null;
    }
    schedule.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeFromTimestamp(startTimestamp, calendar)
    ).setEndTime(Util.getTimeFromTimestamp(time, calendar));
    if (ratingCount > 0) {
      schedule.getSpecBuilder().setRating(ratingSum / ratingCount);
    }
    if (priceLevelCount > 0) {
      schedule.getSpecBuilder().setPriceLevel(priceLevelSum / priceLevelCount);
    }
    if (tight) {
      schedule.setTight(true);
    }
    schedule.setScore(computeScore(schedule));
    return schedule.build();
  }

  private double computeScore(Schedule.Builder schedule) {
    long duration = schedule.getSpec().getTimePeriod().getEndTime().getValue() - schedule.getSpec().getTimePeriod()
        .getStartTime().getValue();
    double score = 24 - TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
    if (schedule.getSpec().getRating() != 0) {
      // half hour is in parity with 1 point.
      score += schedule.getSpec().getRating() / 2;
    }
    if (schedule.getTight() == true) {
      score -= 1;
    }
    return score;
  }

  // Return -1 if can't schedule
  private long getEventDuration(Event.Type type, long time, TimeSlot candidate, boolean tight) {
    long endTime = candidate.getSpec().getTimePeriod().getEndTime().getValue();
    long duration = 0;
    if (type == Event.Type.FOOD) {
      duration = Math.min(tight ? MIN_FOOD_DURATION : DEFAULT_FOOD_DURATION, endTime - time);
      if (duration < MIN_FOOD_DURATION) {
        return -1;
      }
    } else if (type == Event.Type.PLACE) {
      duration = Math.min(tight ? MIN_PLACE_DURATION : DEFAULT_PLACE_DURATION, endTime - time);
      if (duration < MIN_PLACE_DURATION) {
        return -1;
      }
    } else if (type == Event.Type.MOVIE) {
      if (time - candidate.getSpec().getTimePeriod().getStartTime().getValue() > MOVIE_LATE_THRESHOLD) {
        return -1;
      }
      duration = endTime - time;
      if (duration < 0) {
        return -1;
      }
    }
    return duration;
  }

  // Return end time in milli second.
  private long addTimeSlot(long startTime, long duration, Event event, Location startLoc, Location endLoc, Schedule
      .Builder schedule) {
    long endTime = startTime + duration;
    TimeSlot.Builder timeSlot = TimeSlot.newBuilder();
    timeSlot.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeFromTimestamp(startTime, calendar))
        .setEndTime(Util.getTimeFromTimestamp(endTime, calendar));
    timeSlot.getSpecBuilder().setStartLoc(startLoc).setEndLoc(endLoc);
    if (event != null) {
      timeSlot.getEventBuilder().setContent(event.getContent()).setType(event.getType());
    } else {
      timeSlot.getEventBuilder().setType(Event.Type.TRANSPORT);
    }
    schedule.addTimeSlot(timeSlot.build());
    return endTime;
  }

  private void swap(int i, int j) {
    int tmp = permutation[i];
    permutation[i] = permutation[j];
    permutation[j] = tmp;
  }

  private boolean getNextPermutation() {
    if (eventCount == 1) {
      return false;
    }
    int k = -1;
    for (int i = 0; i < eventCount - 1; ++i) {
      if (permutation[i] < permutation[i + 1]) {
        k = i;
      }
    }
    if (k == -1) {
      return false;
    }
    int l = k + 1;
    for (int i = k + 2; i < eventCount; ++i) {
      if (permutation[k] < permutation[i]) {
        l = i;
      }
    }
    swap(k, l);
    int m = k + 1, n = eventCount - 1;
    while (m < n) {
      swap(m, n);
      m++;
      n--;
    }
    return true;
  }

  private boolean getNextIndex() {
    for (int i = size.length - 1; i >= 0; --i) {
      if (index[i] + 1 < size[i]) {
        index[i] = index[i] + 1;
        return true;
      }
      index[i] = 0;
    }
    return false;
  }
}
