package com.helpflow.ticket.domain.model;

public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public static TicketStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TicketStatus cannot be null or empty");
        }
        String clean = value.trim().toUpperCase();
        return switch (clean) {
            case "OPEN", "ABIERTO" -> OPEN;
            case "ASSIGNED", "ASIGNADO" -> ASSIGNED;
            case "IN_PROGRESS", "EN_PROGRESO", "EN PROGRESO" -> IN_PROGRESS;
            case "RESOLVED", "RESUELTO" -> RESOLVED;
            case "CLOSED", "CERRADO" -> CLOSED;
            case "CANCELLED", "CANCELED", "CANCELADO" -> CANCELLED;
            default -> TicketStatus.valueOf(clean);
        };
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
