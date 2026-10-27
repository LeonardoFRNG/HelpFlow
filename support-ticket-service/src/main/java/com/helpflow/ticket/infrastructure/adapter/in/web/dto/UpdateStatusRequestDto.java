package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequestDto(
        @JsonAlias("estado")
        @NotBlank(message = "Status is required (OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED)")
        String status,

        @JsonAlias("observaciones")
        @NotBlank(message = "Observations are required when changing ticket status")
        @Size(min = 3, max = 1000, message = "Observations must be between 3 and 1000 characters")
        String observations
) {}
