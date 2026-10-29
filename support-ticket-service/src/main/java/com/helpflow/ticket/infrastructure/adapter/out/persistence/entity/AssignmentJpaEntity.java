package com.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class AssignmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketJpaEntity ticket;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "technician_code", nullable = false, length = 50)
    private String technicianCode;

    @Column(name = "assigned_by", nullable = false, length = 150)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public AssignmentJpaEntity() {
    }

    public AssignmentJpaEntity(Long id, TicketJpaEntity ticket, Long technicianId, String technicianCode,
                               String assignedBy, LocalDateTime assignedAt, String notes) {
        this.id = id;
        this.ticket = ticket;
        this.technicianId = technicianId;
        this.technicianCode = technicianCode;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketJpaEntity getTicket() {
        return ticket;
    }

    public void setTicket(TicketJpaEntity ticket) {
        this.ticket = ticket;
    }

    public Long getTicketId() {
        return ticket != null ? ticket.getId() : null;
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
}
