package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    List<Ticket> findAll();

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByAssignedTechnicianId(Long technicianId);

    List<Ticket> findByFilters(Long userId, Long technicianId, TicketStatus status, Priority priority, Category category);

    int countActiveTicketsByTechnicianId(Long technicianId);

    List<Ticket> findOpenAndAssignedTickets();

    List<Ticket> findOverdueTickets(LocalDateTime now);
}
