package com.helpflow.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DispatchRequest(
        @NotNull(message = "ticketId is required")
        @Positive(message = "ticketId must be positive")
        Long ticketId,

        @JsonAlias("categoria")
        @NotNull(message = "category (or categoria) is required")
        String category,

        @JsonAlias("prioridad")
        @NotNull(message = "priority (or prioridad) is required")
        String priority
) {}
