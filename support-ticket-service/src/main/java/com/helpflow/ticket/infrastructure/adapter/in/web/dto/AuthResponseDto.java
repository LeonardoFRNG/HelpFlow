package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Role;

public record AuthResponseDto(
        String token,
        String refreshToken,
        String tokenType,
        String email,
        String name,
        Role role,
        Long userId
) {
    public static AuthResponseDto of(String token, String refreshToken, String email, String name, Role role, Long userId) {
        return new AuthResponseDto(token, refreshToken, "Bearer", email, name, role, userId);
    }

    public static AuthResponseDto of(String token, String email, String name, Role role, Long userId) {
        return new AuthResponseDto(token, null, "Bearer", email, name, role, userId);
    }
}
