package com.helpflow.ticket.infrastructure.observability;

import com.helpflow.ticket.application.port.in.EscalateTicketUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SLA Escalation Scheduler Unit Tests")
class SlaEscalationSchedulerTest {

    @Mock
    private EscalateTicketUseCase escalateTicketUseCase;

    private SlaEscalationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SlaEscalationScheduler(escalateTicketUseCase, 30L);
    }

    @Test
    @DisplayName("Scheduler successfully runs monitor job")
    void testSchedulerRuns() {
        when(escalateTicketUseCase.checkAndEscalateApproachingBreachTickets(anyLong())).thenReturn(2);

        scheduler.monitorAndEscalateSlaBreaches();

        verify(escalateTicketUseCase).checkAndEscalateApproachingBreachTickets(anyLong());
    }

    @Test
    @DisplayName("Scheduler handles exceptions gracefully without failing silently")
    void testSchedulerHandlesException() {
        when(escalateTicketUseCase.checkAndEscalateApproachingBreachTickets(anyLong()))
                .thenThrow(new RuntimeException("Database timeout"));

        scheduler.monitorAndEscalateSlaBreaches();

        verify(escalateTicketUseCase).checkAndEscalateApproachingBreachTickets(anyLong());
    }
}
