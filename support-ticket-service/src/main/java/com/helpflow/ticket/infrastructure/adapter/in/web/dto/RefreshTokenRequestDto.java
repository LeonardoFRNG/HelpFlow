package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token is required")
        @Schema(description = "Raw Refresh Token provided upon authentication", example = "dGhpcy1pcy1hLXNlY3VyZS1yZWZyZXNoLXRva2Vu")
        String refreshToken
) {}
