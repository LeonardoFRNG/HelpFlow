package com.helpflow.ticket.domain.exception;

public class TechnicianNotFoundException extends DomainException {
    public TechnicianNotFoundException(Long id) {
        super("Technician not found with id: " + id);
    }

    public TechnicianNotFoundException(String code) {
        super("Technician not found with code: " + code);
    }
}
