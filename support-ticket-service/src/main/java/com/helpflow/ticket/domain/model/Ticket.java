package com.helpflow.ticket.domain.model;

import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.InvalidStateTransitionException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Ticket {
    private Long id;
    private Long userId;
    private String userEmail;
    private String title;
    private String description;
    private Category category;
    private Priority priority;
    private String location;
    private String affectedAsset;
    private LocalDateTime createdAt;
    private LocalDateTime slaDeadline;
    private TicketStatus status;
    private Long assignedTechnicianId;
    private String assignedTechnicianCode;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    private List<TicketHistory> histories = new ArrayList<>();
    private List<Assignment> assignments = new ArrayList<>();

    public Ticket() {
    }

    public Ticket(Long id, Long userId, String userEmail, String title, String description,
                  Category category, Priority priority, String location, String affectedAsset,
                  LocalDateTime createdAt, LocalDateTime slaDeadline, TicketStatus status,
                  Long assignedTechnicianId, String assignedTechnicianCode,
                  LocalDateTime firstResponseAt, LocalDateTime resolvedAt, LocalDateTime closedAt,
                  List<TicketHistory> histories, List<Assignment> assignments) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.location = location;
        this.affectedAsset = affectedAsset;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.slaDeadline = slaDeadline;
        this.status = status != null ? status : TicketStatus.OPEN;
        this.assignedTechnicianId = assignedTechnicianId;
        this.assignedTechnicianCode = assignedTechnicianCode;
        this.firstResponseAt = firstResponseAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        if (histories != null) {
            this.histories.addAll(histories);
        }
        if (assignments != null) {
            this.assignments.addAll(assignments);
        }
    }

    public static Ticket createNew(Long userId, String userEmail, String title, String description,
                                   Category category, Priority priority, String location,
                                   String affectedAsset, LocalDateTime slaDeadline, LocalDateTime createdAt) {
        if (userId == null) {
            throw new BusinessRuleException("User ID is required to create a ticket");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessRuleException("Ticket title cannot be empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessRuleException("Ticket description cannot be empty");
        }
        if (category == null) {
            throw new BusinessRuleException("Category is required");
        }
        if (priority == null) {
            throw new BusinessRuleException("Priority is required");
        }
        LocalDateTime now = createdAt != null ? createdAt : LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setUserEmail(userEmail);
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setLocation(location != null ? location.trim() : null);
        ticket.setAffectedAsset(affectedAsset != null ? affectedAsset.trim() : null);
        ticket.setCreatedAt(now);
        ticket.setSlaDeadline(slaDeadline);
        ticket.setStatus(TicketStatus.OPEN);

        TicketHistory initialHistory = TicketHistory.create(
                null,
                null,
                TicketStatus.OPEN,
                userEmail != null ? userEmail : "SYSTEM",
                "Ticket created in OPEN status",
                now
        );
        ticket.histories.add(initialHistory);

        return ticket;
    }

    public void assignTechnician(Long technicianId, String technicianCode, String assignedBy,
                                 String notes, LocalDateTime now) {
        if (technicianId == null || technicianCode == null || technicianCode.trim().isEmpty()) {
            throw new BusinessRuleException("Valid technician ID and code are required for assignment");
        }
        if (this.status.isTerminal()) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot assign technician to ticket in terminal status %s", this.status));
        }

        TicketStatus prev = this.status;
        this.assignedTechnicianId = technicianId;
        this.assignedTechnicianCode = technicianCode.trim().toUpperCase();
        this.status = TicketStatus.ASSIGNED;

        LocalDateTime timestamp = now != null ? now : LocalDateTime.now();
        Assignment assignment = Assignment.create(this.id, technicianId, this.assignedTechnicianCode,
                assignedBy, notes, timestamp);
        this.assignments.add(assignment);

        String historyNote = notes != null && !notes.isBlank() ? notes : "Assigned to technician " + this.assignedTechnicianCode;
        TicketHistory history = TicketHistory.create(this.id, prev, TicketStatus.ASSIGNED, assignedBy, historyNote, timestamp);
        this.histories.add(history);
    }

    public void updateStatus(TicketStatus targetStatus, String changedBy, String observations, LocalDateTime now) {
        if (targetStatus == null) {
            throw new BusinessRuleException("Target status cannot be null");
        }
        if (observations == null || observations.trim().isEmpty()) {
            throw new BusinessRuleException("Observations are mandatory when changing ticket status");
        }
        if (this.status.isTerminal()) {
            throw new InvalidStateTransitionException(
                    String.format("Ticket %d is in terminal status %s and cannot be modified", this.id, this.status));
        }

        validateTransition(this.status, targetStatus);

        LocalDateTime timestamp = now != null ? now : LocalDateTime.now();
        TicketStatus prev = this.status;


        if (targetStatus == TicketStatus.IN_PROGRESS) {
            if (this.assignedTechnicianId == null) {
                throw new BusinessRuleException("Cannot move ticket to IN_PROGRESS without an assigned technician");
            }
            if (this.firstResponseAt == null) {
                this.firstResponseAt = timestamp;
            }
        } else if (targetStatus == TicketStatus.RESOLVED) {
            this.resolvedAt = timestamp;
        } else if (targetStatus == TicketStatus.CLOSED) {
            this.closedAt = timestamp;
        }

        this.status = targetStatus;

        TicketHistory history = TicketHistory.create(this.id, prev, targetStatus, changedBy, observations.trim(), timestamp);
        this.histories.add(history);
    }

    private void validateTransition(TicketStatus current, TicketStatus target) {
        if (current == target) {
            return;
        }
        boolean valid = switch (current) {
            case OPEN -> target == TicketStatus.ASSIGNED || target == TicketStatus.CANCELLED;
            case ASSIGNED -> target == TicketStatus.IN_PROGRESS || target == TicketStatus.CANCELLED || target == TicketStatus.ASSIGNED;
            case IN_PROGRESS -> target == TicketStatus.RESOLVED || target == TicketStatus.ASSIGNED || target == TicketStatus.CANCELLED;
            case RESOLVED -> target == TicketStatus.CLOSED || target == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(current, target);
        }
    }

    public void escalate(Priority newPriority, LocalDateTime newDeadline, String changedBy, String reason, LocalDateTime now) {
        if (newPriority == null) {
            throw new BusinessRuleException("Escalation requires a valid target priority");
        }
        if (this.status.isTerminal()) {
            throw new BusinessRuleException("Cannot escalate a closed or cancelled ticket");
        }
        LocalDateTime timestamp = now != null ? now : LocalDateTime.now();
        Priority oldPriority = this.priority;
        this.priority = newPriority;
        if (newDeadline != null) {
            this.slaDeadline = newDeadline;
        }

        String obs = String.format("Ticket escalated from %s to %s. Reason: %s",
                oldPriority, newPriority, reason != null ? reason : "SLA breach risk");
        TicketHistory history = TicketHistory.create(this.id, this.status, this.status, changedBy, obs, timestamp);
        this.histories.add(history);
    }

    public boolean isOverdue(LocalDateTime now) {
        if (this.status == TicketStatus.RESOLVED || this.status.isTerminal()) {
            return false;
        }
        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();
        return this.slaDeadline != null && this.slaDeadline.isBefore(checkTime);
    }

    public boolean isApproachingBreach(LocalDateTime now, long thresholdMinutes) {
        if (this.status == TicketStatus.RESOLVED || this.status.isTerminal() || this.slaDeadline == null) {
            return false;
        }
        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();
        if (isOverdue(checkTime)) {
            return false;
        }
        return checkTime.plusMinutes(thresholdMinutes).isAfter(this.slaDeadline);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAffectedAsset() {
        return affectedAsset;
    }

    public void setAffectedAsset(String affectedAsset) {
        this.affectedAsset = affectedAsset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Long assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedTechnicianCode() {
        return assignedTechnicianCode;
    }

    public void setAssignedTechnicianCode(String assignedTechnicianCode) {
        this.assignedTechnicianCode = assignedTechnicianCode;
    }

    public LocalDateTime getFirstResponseAt() {
        return firstResponseAt;
    }

    public void setFirstResponseAt(LocalDateTime firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public List<TicketHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }

    public void setHistories(List<TicketHistory> histories) {
        this.histories = histories != null ? new ArrayList<>(histories) : new ArrayList<>();
    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments != null ? new ArrayList<>(assignments) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
