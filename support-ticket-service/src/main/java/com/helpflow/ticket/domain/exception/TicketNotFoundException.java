package com.helpflow.ticket.domain.exception;

public class TicketNotFoundException extends DomainException {
    public TicketNotFoundException(Long id) {
        super("Ticket not found with id: " + id);
    }
}
