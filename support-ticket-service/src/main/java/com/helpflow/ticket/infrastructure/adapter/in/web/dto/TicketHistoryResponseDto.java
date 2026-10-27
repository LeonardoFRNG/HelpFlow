package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.TicketStatus;
import java.time.LocalDateTime;

public record TicketHistoryResponseDto(
        Long id,
        Long ticketId,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        String changedBy,
        LocalDateTime changedAt,
        String observations
) {}
