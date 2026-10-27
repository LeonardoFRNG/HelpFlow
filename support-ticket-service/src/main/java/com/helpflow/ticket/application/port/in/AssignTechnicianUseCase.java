package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Ticket;

public interface AssignTechnicianUseCase {

    record AssignTechnicianCommand(
            Long ticketId,
            String technicianCode,
            String assignedByEmail,
            String notes
    ) {}

    Ticket assignTechnician(AssignTechnicianCommand command);
}
