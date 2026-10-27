package com.helpflow.ticket.domain.model;

public enum UserStatus {
    ACTIVE,
    INACTIVE;

    public static UserStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        String clean = value.trim().toUpperCase();
        if (clean.equals("ACTIVO") || clean.equals("ACTIVE")) {
            return ACTIVE;
        }
        if (clean.equals("INACTIVO") || clean.equals("INACTIVE")) {
            return INACTIVE;
        }
        return UserStatus.valueOf(clean);
    }
}
