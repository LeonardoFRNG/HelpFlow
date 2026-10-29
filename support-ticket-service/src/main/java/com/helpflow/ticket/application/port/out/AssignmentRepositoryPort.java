package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Assignment;

import java.util.List;

public interface AssignmentRepositoryPort {
    Assignment save(Assignment assignment);

    List<Assignment> findByTicketId(Long ticketId);
}
