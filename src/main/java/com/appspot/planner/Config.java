package com.appspot.planner;

import com.appspot.planner.proto.PlannerProtos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Config {
  private static Config config = new Config();
  private Map<String, Integer> defaultDurations;  // in min

  public static final long DEFAULT_DURATION = 3600000;  // 1 hour

  private Config() {
    defaultDurations = new HashMap<>();
    defaultDurations.put("restaurant", 60);
    defaultDurations.put("shopping_mall", 120);
    defaultDurations.put("night_club", 120);
    defaultDurations.put("store", 20);
    defaultDurations.put("bank", 30);
    defaultDurations.put("beauty_salon", 60);
    defaultDurations.put("casino", 120);
    defaultDurations.put("convenience_store", 20);
    defaultDurations.put("gas_station", 10);
    defaultDurations.put("hospital", 60);
    defaultDurations.put("park", 60);
    defaultDurations.put("spa", 120);
    defaultDurations.put("cafe", 30);
  }

  public static Config getInstance() {
    return config;
  }

  public long getSuggestedDuration(PlannerProtos.TimeSlotOrBuilder timeSlot) {
    long typeDuration = -1;
    for (String type : timeSlot.getSpec().getTypesList()) {
      if (defaultDurations.containsKey(type)) {
        typeDuration = TimeUnit.MILLISECONDS.convert(defaultDurations.get(type), TimeUnit.MINUTES);
        break;
      }
    }
    return typeDuration == -1 ? DEFAULT_DURATION : typeDuration;
  }
}
