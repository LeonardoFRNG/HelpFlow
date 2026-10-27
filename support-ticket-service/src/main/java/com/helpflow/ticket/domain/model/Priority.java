package com.helpflow.ticket.domain.model;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Priority cannot be null or empty");
        }
        String clean = value.trim().toUpperCase();
        return switch (clean) {
            case "LOW", "BAJA", "BAJO" -> LOW;
            case "MEDIUM", "MEDIA", "MEDIO" -> MEDIUM;
            case "HIGH", "ALTA", "ALTO" -> HIGH;
            case "CRITICAL", "CRITICA", "CRÍTICA", "CRITICO", "CRÍTICO" -> CRITICAL;
            default -> Priority.valueOf(clean);
        };
    }
}
