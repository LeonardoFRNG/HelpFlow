package com.helpflow.ticket.infrastructure.observability;

import com.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class HelpFlowMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter authFailuresCounter;
    private final Counter accessDeniedCounter;
    private final Timer firstResponseTimer;
    private final Timer resolutionTimer;

    public HelpFlowMetrics(MeterRegistry meterRegistry, TicketRepositoryPort ticketRepository) {
        this.meterRegistry = meterRegistry;

        this.authFailuresCounter = Counter.builder("helpflow.auth.failures")
                .description("Total number of failed authentication attempts")
                .register(meterRegistry);

        this.accessDeniedCounter = Counter.builder("helpflow.auth.access_denied")
                .description("Total number of access denied events")
                .register(meterRegistry);

        this.firstResponseTimer = Timer.builder("helpflow.tickets.first_response.time")
                .description("Time taken from ticket creation to first technician response")
                .register(meterRegistry);

        this.resolutionTimer = Timer.builder("helpflow.tickets.resolution.time")
                .description("Time taken from ticket creation to resolution")
                .register(meterRegistry);

        // Overdue tickets gauge
        Gauge.builder("helpflow.tickets.overdue", () -> {
            try {
                return ticketRepository.findOverdueTickets(LocalDateTime.now()).size();
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Current number of overdue tickets according to SLA")
        .register(meterRegistry);
    }

    public void recordTicketCreated(Category category, Priority priority) {
        Counter.builder("helpflow.tickets.created")
                .tag("category", category != null ? category.name() : "UNKNOWN")
                .tag("priority", priority != null ? priority.name() : "UNKNOWN")
                .description("Total tickets created by category and priority")
                .register(meterRegistry)
                .increment();
    }

    public void recordFirstResponseTime(Duration duration) {
        if (duration != null && !duration.isNegative()) {
            firstResponseTimer.record(duration);
        }
    }

    public void recordResolutionTime(Duration duration) {
        if (duration != null && !duration.isNegative()) {
            resolutionTimer.record(duration);
        }
    }

    public void recordAuthFailure() {
        authFailuresCounter.increment();
    }

    public void recordAccessDenied() {
        accessDeniedCounter.increment();
    }
}
