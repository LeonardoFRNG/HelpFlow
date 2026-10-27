package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.policy.SlaPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SLA Policy Tests")
class SlaPolicyTest {

    private final SlaPolicy slaPolicy = new SlaPolicy();

    @Test
    @DisplayName("SLA duration corresponds to business priority rules")
    void testSlaDurations() {
        assertEquals(Duration.ofHours(2), slaPolicy.getDurationFor(Priority.CRITICAL));
        assertEquals(Duration.ofHours(4), slaPolicy.getDurationFor(Priority.HIGH));
        assertEquals(Duration.ofHours(8), slaPolicy.getDurationFor(Priority.MEDIUM));
        assertEquals(Duration.ofHours(24), slaPolicy.getDurationFor(Priority.LOW));
    }

    @Test
    @DisplayName("Deadline calculation adds duration to reference time")
    void testDeadlineCalculation() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 27, 8, 0, 0);

        LocalDateTime criticalDeadline = slaPolicy.calculateDeadline(Priority.CRITICAL, baseTime);
        assertEquals(LocalDateTime.of(2026, 10, 27, 10, 0, 0), criticalDeadline);

        LocalDateTime lowDeadline = slaPolicy.calculateDeadline(Priority.LOW, baseTime);
        assertEquals(LocalDateTime.of(2026, 10, 28, 8, 0, 0), lowDeadline);
    }
}
