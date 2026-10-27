package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Role;

public record AuthResponseDto(
        String token,
        String tokenType,
        String email,
        String name,
        Role role,
        Long userId
) {
    public static AuthResponseDto of(String token, String email, String name, Role role, Long userId) {
        return new AuthResponseDto(token, "Bearer", email, name, role, userId);
    }
}
