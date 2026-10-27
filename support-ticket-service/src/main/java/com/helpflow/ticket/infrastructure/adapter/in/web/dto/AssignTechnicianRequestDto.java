package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AssignTechnicianRequestDto(
        @JsonAlias({"technicianId", "tecnicoId", "code"})
        @NotBlank(message = "technicianCode is required (e.g. TEC-001)")
        String technicianCode,

        @JsonAlias({"notas", "observations"})
        String notes
) {}
