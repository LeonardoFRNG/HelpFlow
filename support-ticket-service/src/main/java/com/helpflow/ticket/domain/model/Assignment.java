package com.helpflow.ticket.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Assignment {
    private Long id;
    private Long ticketId;
    private Long technicianId;
    private String technicianCode;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private String notes;

    public Assignment() {
    }

    public Assignment(Long id, Long ticketId, Long technicianId, String technicianCode,
                      String assignedBy, LocalDateTime assignedAt, String notes) {
        this.id = id;
        this.ticketId = ticketId;
        this.technicianId = technicianId;
        this.technicianCode = technicianCode;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.notes = notes;
    }

    public static Assignment create(Long ticketId, Long technicianId, String technicianCode,
                                    String assignedBy, String notes, LocalDateTime assignedAt) {
        return new Assignment(null, ticketId, technicianId, technicianCode, assignedBy,
                assignedAt != null ? assignedAt : LocalDateTime.now(), notes);
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

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getTechnicianCode() {
        return technicianCode;
    }

    public void setTechnicianCode(String technicianCode) {
        this.technicianCode = technicianCode;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
