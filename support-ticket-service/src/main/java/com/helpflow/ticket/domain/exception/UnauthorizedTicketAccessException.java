package com.helpflow.ticket.domain.exception;

public class UnauthorizedTicketAccessException extends DomainException {
    public UnauthorizedTicketAccessException(String message) {
        super(message);
    }

    public UnauthorizedTicketAccessException(Long ticketId, String username) {
        super(String.format("User '%s' is not authorized to access ticket id %d", username, ticketId));
    }
}
