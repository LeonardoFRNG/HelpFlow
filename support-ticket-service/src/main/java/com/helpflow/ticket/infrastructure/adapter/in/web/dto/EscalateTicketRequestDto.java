package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record EscalateTicketRequestDto(
        @JsonAlias("nuevaPrioridad")
        @NotBlank(message = "targetPriority is required (LOW, MEDIUM, HIGH, CRITICAL)")
        String targetPriority,

        @JsonAlias({"motivo", "observaciones"})
        String reason
) {}
