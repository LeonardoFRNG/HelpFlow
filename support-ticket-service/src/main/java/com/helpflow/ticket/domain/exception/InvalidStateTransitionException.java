package com.helpflow.ticket.domain.exception;

import com.helpflow.ticket.domain.model.TicketStatus;

public class InvalidStateTransitionException extends DomainException {
    private final TicketStatus currentStatus;
    private final TicketStatus attemptedStatus;

    public InvalidStateTransitionException(TicketStatus currentStatus, TicketStatus attemptedStatus) {
        super(String.format("Invalid state transition from %s to %s", currentStatus, attemptedStatus));
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

    public InvalidStateTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.attemptedStatus = null;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}
