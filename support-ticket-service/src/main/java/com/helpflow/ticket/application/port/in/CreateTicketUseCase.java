package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;

public interface CreateTicketUseCase {

    record CreateTicketCommand(
            String requesterEmail,
            String title,
            String description,
            Category category,
            Priority priority,
            String location,
            String affectedAsset
    ) {}

    Ticket createTicket(CreateTicketCommand command);
}
