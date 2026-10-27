package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Document is required")
        @Size(min = 3, max = 50, message = "Document must be between 3 and 50 characters")
        String document,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Corporate email is required")
        @Email(message = "Email must be a valid format")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@helpflow\\.com$",
                message = "Email must belong to corporate domain @helpflow.com")
        String corporateEmail,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        String location,

        String area,

        Role role
) {}
