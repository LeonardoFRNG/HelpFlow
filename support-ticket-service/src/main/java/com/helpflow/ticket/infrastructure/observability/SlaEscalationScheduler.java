package com.helpflow.ticket.infrastructure.observability;

import com.helpflow.ticket.application.port.in.EscalateTicketUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SlaEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaEscalationScheduler.class);

    private final EscalateTicketUseCase escalateTicketUseCase;
    private final long thresholdMinutes;

    public SlaEscalationScheduler(
            EscalateTicketUseCase escalateTicketUseCase,
            @Value("${sla.check.threshold-minutes:30}") long thresholdMinutes) {
        this.escalateTicketUseCase = escalateTicketUseCase;
        this.thresholdMinutes = thresholdMinutes;
    }

    @Scheduled(cron = "${sla.check.cron:0 */5 * * * *}")
    public void monitorAndEscalateSlaBreaches() {
        String schedulerTraceId = "sched-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", schedulerTraceId);

        try {
            log.info("Running SLA monitoring job (threshold: {} minutes)", thresholdMinutes);
            int escalated = escalateTicketUseCase.checkAndEscalateApproachingBreachTickets(thresholdMinutes);
            if (escalated > 0) {
                log.warn("SLA monitor automatically escalated {} tickets approaching SLA breach", escalated);
            } else {
                log.info("SLA monitor found 0 tickets requiring escalation");
            }
        } catch (Exception ex) {
            log.error("Error executing SLA escalation job: {}", ex.getMessage(), ex);
        } finally {
            MDC.remove("traceId");
        }
    }
}
