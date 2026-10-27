package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String document,
        String name,
        String corporateEmail,
        String location,
        String area,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static UserResponseDto fromDomain(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getDocument(),
                user.getName(),
                user.getCorporateEmail(),
                user.getLocation(),
                user.getArea(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
