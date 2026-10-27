package com.helpflow.ticket.domain.policy;

import com.helpflow.ticket.domain.model.Priority;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class SlaPolicy {
    private final Map<Priority, Duration> slaDurations = new EnumMap<>(Priority.class);

    public SlaPolicy() {
        // Default business SLA standards:
        // CRITICAL: 2 hours
        // HIGH: 4 hours
        // MEDIUM: 8 hours
        // LOW: 24 hours
        slaDurations.put(Priority.CRITICAL, Duration.ofHours(2));
        slaDurations.put(Priority.HIGH, Duration.ofHours(4));
        slaDurations.put(Priority.MEDIUM, Duration.ofHours(8));
        slaDurations.put(Priority.LOW, Duration.ofHours(24));
    }

    public SlaPolicy(Duration criticalDuration, Duration highDuration, Duration mediumDuration, Duration lowDuration) {
        slaDurations.put(Priority.CRITICAL, criticalDuration != null ? criticalDuration : Duration.ofHours(2));
        slaDurations.put(Priority.HIGH, highDuration != null ? highDuration : Duration.ofHours(4));
        slaDurations.put(Priority.MEDIUM, mediumDuration != null ? mediumDuration : Duration.ofHours(8));
        slaDurations.put(Priority.LOW, lowDuration != null ? lowDuration : Duration.ofHours(24));
    }

    public Duration getDurationFor(Priority priority) {
        if (priority == null) {
            return Duration.ofHours(8);
        }
        return slaDurations.getOrDefault(priority, Duration.ofHours(8));
    }

    public LocalDateTime calculateDeadline(Priority priority, LocalDateTime from) {
        LocalDateTime baseTime = from != null ? from : LocalDateTime.now();
        Duration duration = getDurationFor(priority);
        return baseTime.plus(duration);
    }
}
