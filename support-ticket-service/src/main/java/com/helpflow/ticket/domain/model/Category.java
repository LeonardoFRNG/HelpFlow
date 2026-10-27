package com.helpflow.ticket.domain.model;

public enum Category {
    HARDWARE,
    SOFTWARE,
    NETWORK,
    ACCESS,
    OTHER;

    public static Category fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        String clean = value.trim().toUpperCase();
        return switch (clean) {
            case "HARDWARE" -> HARDWARE;
            case "SOFTWARE" -> SOFTWARE;
            case "NETWORK", "RED" -> NETWORK;
            case "ACCESS", "ACCESOS", "ACCESO" -> ACCESS;
            case "OTHER", "OTRO", "OTROS" -> OTHER;
            default -> Category.valueOf(clean);
        };
    }
}
