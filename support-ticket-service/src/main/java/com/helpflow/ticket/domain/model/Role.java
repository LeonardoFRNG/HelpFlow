package com.helpflow.ticket.domain.model;

public enum Role {
    ROLE_USUARIO,
    ROLE_TECNICO,
    ROLE_ADMIN;

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return ROLE_USUARIO;
        }
        String clean = value.trim().toUpperCase();
        if (clean.equals("USUARIO") || clean.equals("USER") || clean.equals("ROLE_USUARIO") || clean.equals("ROLE_USER")) {
            return ROLE_USUARIO;
        }
        if (clean.equals("TECNICO") || clean.equals("TECHNICIAN") || clean.equals("ROLE_TECNICO") || clean.equals("ROLE_TECHNICIAN")) {
            return ROLE_TECNICO;
        }
        if (clean.equals("ADMIN") || clean.equals("ADMINISTRATOR") || clean.equals("ROLE_ADMIN")) {
            return ROLE_ADMIN;
        }
        return Role.valueOf(clean);
    }
}
