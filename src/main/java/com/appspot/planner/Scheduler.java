package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos.*;
import com.appspot.planner.util.Util;

import java.util.Calendar;

public class Scheduler {

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
  Requirement.TravelMode travelMode;

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
    endLoc = !request.getRequirement().getEndLoc().getAddress().equals("") ? request.getRequirement().getEndLoc() :
        startLoc;
    startLoc = GoogleGeoAPI.getLocation(startLoc);
    endLoc = GoogleGeoAPI.getLocation(endLoc);
    startTimestamp = calendar.getTimeInMillis();
    endTimestamp = request.getRequirement().getTimePeriod().getEndTime().getText().isEmpty() ? 0 : Util
        .getCalendarFromTime(request.getRequirement().getTimePeriod().getEndTime(), calendar.getTimeZone())
        .getTimeInMillis();
    travelMode = request.getRequirement().getTravelMode();
  }

  public GetPlanResponse.Builder getSchedule() {
    if (startLoc == null || endLoc == null) {
      return response;
    }
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
        Schedule schedule = trySchedule(false, false);
        if (schedule != null) {
          response.addScheduleCandidate(schedule);
          continue;
        }
        schedule = trySchedule(true, false);
        if (schedule != null) {
          response.addScheduleCandidate(schedule);
        }
      } while (getNextPermutation());
    } while (getNextIndex());
    if (response.getScheduleCandidateCount() == 0) {
      return response;
    }
    Schedule best = null;
    for (Schedule schedule : response.getScheduleCandidateList()) {
      if (best == null || schedule.getScore() > best.getScore()) {
        best = schedule;
      }
    }
    recoverStatus(best);
    Schedule bestAccurate = trySchedule(best.getTight(), true);
    if (bestAccurate != null) {
      response.setSchedule(bestAccurate);
    } else if (!best.getTight()) {
      bestAccurate = trySchedule(true, true);
      if (bestAccurate != null) {
        response.setSchedule(bestAccurate);
      } else {
        // response.setSchedule(best);
      }
    } else {
      // response.setSchedule(best);
    }
    return response;
  }

  // Return null if can't schedule
  // Allow half duration if tight is true
  // Use actual GeoMatrix to compute transport duration if accurate is true
  private Schedule trySchedule(boolean tight, boolean accurate) {
    Schedule.Builder schedule = Schedule.newBuilder();
    long time = startTimestamp;
    Location previousLoc = startLoc;
    double ratingSum = 0;
    int ratingCount = 0;
    double priceLevelSum = 0;
    int priceLevelCount = 0;
    long transportDuration = 0;
    for (int i = 0; i < eventCount; ++i) {
      Event event = response.getProcessedEvent(permutation[i]);
      TimeSlot candidate = event.getCandicates(index[permutation[i]]);
      Location eventLoc = candidate.getSpec().getStartLoc();
      // Add transport
      long duration = getDuration(previousLoc, eventLoc, accurate);
      transportDuration += duration;
      time = addTimeSlot(time, duration, previousLoc, eventLoc, null, schedule);
      // Add event
      if (time < candidate.getSpec().getTimePeriod().getStartTime().getValue()) {
        time = candidate.getSpec().getTimePeriod().getStartTime().getValue();
      }
      duration = getEventDuration(event.getType(), time, candidate, tight);
      if (duration < 0) {
        return null;
      }
      time = addTimeSlot(time, duration, eventLoc, null, candidate, schedule);
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
    long duration = getDuration(previousLoc, endLoc, accurate);
    transportDuration += transportDuration;
    time = addTimeSlot(time, duration, previousLoc, endLoc, null, schedule);
    if (endTimestamp > 0 && time > endTimestamp) {
      return null;
    }

    long optimizedStartTimestamp = OptimizeSchedule(schedule);

    // Write summary
    schedule.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeFromTimestamp(optimizedStartTimestamp,
        calendar)).setEndTime(Util.getTimeFromTimestamp(time, calendar));
    transportDuration -= getDuration(startLoc, endLoc, accurate);
    schedule.setTransportDuration(transportDuration);
    schedule.getSpecBuilder().setRating(ratingCount > 0 ? (ratingSum / ratingCount) : 3);
    if (priceLevelCount > 0) {
      schedule.getSpecBuilder().setPriceLevel(priceLevelSum / priceLevelCount);
    }
    if (tight) {
      schedule.setTight(true);
    }
    schedule.setScore(computeScore(schedule));
    if (!accurate) {
      storeStatus(schedule);
    }
    return schedule.build();
  }

  private long getDuration(Location startLoc, Location endLoc, boolean accurate) {
    if (accurate) {
      return GoogleGeoAPI.getDuration(startLoc, endLoc, travelMode);
    } else {
      return Util.getEstimatedDuration(startLoc, endLoc, travelMode);
    }
  }

  private long OptimizeSchedule(Schedule.Builder schedule) {
    long newStartTimestamp = startTimestamp;
    for (int i = schedule.getTimeSlotCount() - 3; i >= 0; --i) {
      TimeSlot.Builder timeSlot = schedule.getTimeSlotBuilder(i);
      TimeSlot.Builder nextTimeSlot = schedule.getTimeSlotBuilder(i + 1);
      if (timeSlot.getEvent().getType() != Event.Type.TRANSPORT) {
        long extend = Math.min(timeSlot.getSpec().getSuggestedDuration() - getEndTime(timeSlot) + getStartTime
            (timeSlot), Math.min(getCloseTime(timeSlot) - getEndTime(timeSlot), getStartTime(nextTimeSlot) -
            getEndTime(timeSlot)));
        if (extend > 0) {
          addEndTime(timeSlot, extend);
        } else {
          long delay = Math.min(getStartTime(nextTimeSlot) - getEndTime(timeSlot), getCloseTime(timeSlot) -
              getEndTime(timeSlot));
          if (delay > 0) {
            addStartTime(timeSlot, delay);
            addEndTime(timeSlot, delay);
          }
        }
      } else {
        long delay = getStartTime(nextTimeSlot) - getEndTime(timeSlot);
        if (delay > 0) {
          addStartTime(timeSlot, delay);
          addEndTime(timeSlot, delay);
        }
      }
      newStartTimestamp = getStartTime(timeSlot);
    }
    for (int i = 2; i < schedule.getTimeSlotCount(); ++i) {
      TimeSlot.Builder timeSlot = schedule.getTimeSlotBuilder(i);
      TimeSlot.Builder lastTimeSlot = schedule.getTimeSlotBuilder(i - 1);
      long gap;
      if (timeSlot.getEvent().getType() != Event.Type.TRANSPORT) {
        gap = Math.min(getStartTime(timeSlot) - getEndTime(lastTimeSlot), getStartTime(timeSlot) - getOpenTime
            (timeSlot));
      } else {
        gap = getStartTime(timeSlot) - getEndTime(lastTimeSlot);
      }
      if (gap > 0) {
        addStartTime(timeSlot, 0 - gap);
        addEndTime(timeSlot, 0 - gap);
      }
    }
    return newStartTimestamp;
  }

  private long getStartTime(TimeSlot.Builder timeSlot) {
    return timeSlot.getSpec().getTimePeriod().getStartTime().getValue();
  }

  private long getEndTime(TimeSlot.Builder timeSlot) {
    return timeSlot.getSpec().getTimePeriod().getEndTime().getValue();
  }

  private long getOpenTime(TimeSlot.Builder timeSlot) {
    return timeSlot.getSpec().getAvailableTimePeriod().getStartTime().getValue();
  }

  private long getCloseTime(TimeSlot.Builder timeSlot) {
    return timeSlot.getSpec().getAvailableTimePeriod().getEndTime().getValue();
  }

  private void addStartTime(TimeSlot.Builder timeSlot, long delta) {
    timeSlot.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeFromTimestamp(timeSlot.getSpec()
        .getTimePeriod().getStartTime().getValue() + delta, calendar));
  }

  private void addEndTime(TimeSlot.Builder timeSlot, long delta) {
    timeSlot.getSpecBuilder().getTimePeriodBuilder().setEndTime(Util.getTimeFromTimestamp(timeSlot.getSpec()
        .getTimePeriod().getEndTime().getValue() + delta, calendar));
  }

  private void storeStatus(Schedule.Builder schedule) {
    for (int i = 0; i < eventCount; ++i) {
      schedule.addIndex(index[i]);
      schedule.addPermutation(permutation[i]);
    }
  }

  private void recoverStatus(Schedule schedule) {
    for (int i = 0; i < eventCount; ++i) {
      index[i] = schedule.getIndex(i);
      permutation[i] = schedule.getPermutation(i);
    }
  }

  private double computeScore(Schedule.Builder schedule) {
    double score = schedule.getSpec().getRating() - (schedule.getSpec().getTimePeriod().getEndTime().getValue() -
        startTimestamp + schedule.getTransportDuration()) / 3600000.0;
    if (schedule.getTight() == true) {
      score -= eventCount;
    }
    return score;
  }

  // Return -1 if can't schedule
  private long getEventDuration(Event.Type eventType, long time, TimeSlot candidate, boolean tight) {
    long endTime = candidate.getSpec().getTimePeriod().getEndTime().getValue();
    if (eventType == Event.Type.MOVIE) {
      if (time - candidate.getSpec().getTimePeriod().getStartTime().getValue() > MOVIE_LATE_THRESHOLD) {
        return -1;
      }
      return endTime - time;
    }
    long duration = candidate.getSpec().getSuggestedDuration();
    if (tight) {
      duration /= 2;
    }
    if (endTime - time < duration) {
      return -1;
    }
    return duration;
  }

  // Return end time in milli second.
  private long addTimeSlot(long startTime, long duration, Location startLoc, Location endLoc, TimeSlot fromTimeSlot,
                           Schedule.Builder schedule) {
    long endTime = startTime + duration;
    TimeSlot.Builder timeSlot = schedule.addTimeSlotBuilder();
    timeSlot.getSpecBuilder().getTimePeriodBuilder().setStartTime(Util.getTimeFromTimestamp(startTime, calendar))
        .setEndTime(Util.getTimeFromTimestamp(endTime, calendar));
    timeSlot.getSpecBuilder().setStartLoc(startLoc);
    if (endLoc != null) {
      timeSlot.getSpecBuilder().setEndLoc(endLoc);
    }
    if (fromTimeSlot != null) {
      timeSlot.getEventBuilder().setContent(fromTimeSlot.getEvent().getContent()).setType(fromTimeSlot.getEvent()
          .getType());
      timeSlot.getSpecBuilder().setPriceLevel(fromTimeSlot.getSpec().getPriceLevel()).setRating(fromTimeSlot.getSpec
          ().getRating());
      timeSlot.getSpecBuilder().addAllTypes(fromTimeSlot.getSpec().getTypesList());
      timeSlot.getSpecBuilder().setAvailableTimePeriod(fromTimeSlot.getSpec().getTimePeriod());
      timeSlot.getSpecBuilder().setSuggestedDuration(fromTimeSlot.getSpec().getSuggestedDuration());
      if (fromTimeSlot.hasPlace()) {
        timeSlot.setPlace(fromTimeSlot.getPlace());
      }
    } else {
      timeSlot.getEventBuilder().setType(Event.Type.TRANSPORT);
    }
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
