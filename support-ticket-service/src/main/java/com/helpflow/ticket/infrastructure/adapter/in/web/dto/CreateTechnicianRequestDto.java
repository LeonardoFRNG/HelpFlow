package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Category;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record CreateTechnicianRequestDto(
        @NotBlank(message = "Technician code is required (e.g., TEC-021)")
        String code,

        @NotBlank(message = "Document is required")
        String document,

        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Email must be valid format")
        String email,

        String location,

        @NotEmpty(message = "At least one specialty is required")
        Set<Category> specialties,

        @Positive(message = "maxActiveTickets must be positive")
        int maxActiveTickets
) {}
