package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequestDto(
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        String description,

        @JsonAlias("categoria")
        @NotBlank(message = "Category is required (HARDWARE, SOFTWARE, NETWORK, ACCESS, OTHER)")
        String category,

        @JsonAlias("prioridad")
        @NotBlank(message = "Priority is required (LOW, MEDIUM, HIGH, CRITICAL)")
        String priority,

        @JsonAlias("sede")
        String location,

        @JsonAlias("activo")
        String affectedAsset
) {}
