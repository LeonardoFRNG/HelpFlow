package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;

import java.util.List;

public interface GetTicketUseCase {

    record TicketFilter(
            TicketStatus status,
            Priority priority,
            Category category
    ) {}

    Ticket getTicketById(Long id, String requesterEmail, Role requesterRole);

    List<Ticket> getTickets(String requesterEmail, Role requesterRole, TicketFilter filter);
}
