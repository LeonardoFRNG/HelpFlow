package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

public record AssignmentResponseDto(
        Long id,
        Long ticketId,
        Long technicianId,
        String technicianCode,
        String assignedBy,
        LocalDateTime assignedAt,
        String notes
) {}
