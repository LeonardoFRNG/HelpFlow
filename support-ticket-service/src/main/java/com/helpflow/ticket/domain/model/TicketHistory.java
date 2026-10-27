package com.helpflow.ticket.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class TicketHistory {
    private Long id;
    private Long ticketId;
    private TicketStatus previousStatus;
    private TicketStatus newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
    private String observations;

    public TicketHistory() {
    }

    public TicketHistory(Long id, Long ticketId, TicketStatus previousStatus, TicketStatus newStatus,
                         String changedBy, LocalDateTime changedAt, String observations) {
        this.id = id;
        this.ticketId = ticketId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.observations = observations;
    }

    public static TicketHistory create(Long ticketId, TicketStatus previousStatus, TicketStatus newStatus,
                                       String changedBy, String observations, LocalDateTime changedAt) {
        return new TicketHistory(null, ticketId, previousStatus, newStatus,
                changedBy, changedAt != null ? changedAt : LocalDateTime.now(), observations);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public TicketStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(TicketStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public TicketStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TicketStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketHistory that = (TicketHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
