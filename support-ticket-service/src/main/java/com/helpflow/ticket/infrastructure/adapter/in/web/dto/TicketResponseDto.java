package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponseDto(
        Long id,
        Long userId,
        String userEmail,
        String title,
        String description,
        Category category,
        Priority priority,
        String location,
        String affectedAsset,
        LocalDateTime createdAt,
        LocalDateTime slaDeadline,
        TicketStatus status,
        Long assignedTechnicianId,
        String assignedTechnicianCode,
        LocalDateTime firstResponseAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        boolean isOverdue,
        List<AssignmentResponseDto> assignments,
        List<TicketHistoryResponseDto> histories
) {
    public static TicketResponseDto fromDomain(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        List<AssignmentResponseDto> assignmentDtos = ticket.getAssignments() != null
                ? ticket.getAssignments().stream()
                .map(a -> new AssignmentResponseDto(a.getId(), a.getTicketId(), a.getTechnicianId(),
                        a.getTechnicianCode(), a.getAssignedBy(), a.getAssignedAt(), a.getNotes()))
                .toList()
                : List.of();

        List<TicketHistoryResponseDto> historyDtos = ticket.getHistories() != null
                ? ticket.getHistories().stream()
                .map(h -> new TicketHistoryResponseDto(h.getId(), h.getTicketId(), h.getPreviousStatus(),
                        h.getNewStatus(), h.getChangedBy(), h.getChangedAt(), h.getObservations()))
                .toList()
                : List.of();

        return new TicketResponseDto(
                ticket.getId(),
                ticket.getUserId(),
                ticket.getUserEmail(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getLocation(),
                ticket.getAffectedAsset(),
                ticket.getCreatedAt(),
                ticket.getSlaDeadline(),
                ticket.getStatus(),
                ticket.getAssignedTechnicianId(),
                ticket.getAssignedTechnicianCode(),
                ticket.getFirstResponseAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.isOverdue(now),
                assignmentDtos,
                historyDtos
        );
    }
}
