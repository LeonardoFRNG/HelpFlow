package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;

public interface UpdateTicketStatusUseCase {

    record UpdateStatusCommand(
            Long ticketId,
            TicketStatus targetStatus,
            String observations,
            String updatedByEmail,
            Role updatedByRole
    ) {}

    Ticket updateStatus(UpdateStatusCommand command);
}
