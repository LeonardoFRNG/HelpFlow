package com.helpflow.ticket.domain.exception;

public class UserInactiveException extends DomainException {
    public UserInactiveException(Long userId) {
        super(String.format("User %d is inactive and cannot perform ticket operations", userId));
    }

    public UserInactiveException(String message) {
        super(message);
    }
}
