package com.helpflow.ticket.domain.model;

public enum Availability {
    HIGH,
    MEDIUM,
    LOW;

    public static Availability fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        String clean = value.trim().toUpperCase();
        if (clean.contains("ALTA") || clean.equals("HIGH")) {
            return HIGH;
        }
        if (clean.contains("MEDIA") || clean.equals("MEDIUM")) {
            return MEDIUM;
        }
        if (clean.contains("BAJA") || clean.equals("LOW")) {
            return LOW;
        }
        return Availability.valueOf(clean);
    }
}
