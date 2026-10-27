package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;

public interface EscalateTicketUseCase {

    record EscalateTicketCommand(
            Long ticketId,
            Priority targetPriority,
            String reason,
            String escalatedByEmail
    ) {}

    Ticket escalateTicket(EscalateTicketCommand command);

    int checkAndEscalateApproachingBreachTickets(long thresholdMinutes);
}
