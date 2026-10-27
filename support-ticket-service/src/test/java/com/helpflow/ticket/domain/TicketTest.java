package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.InvalidStateTransitionException;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ticket Domain Aggregate Tests")
class TicketTest {

    @Test
    @DisplayName("Creating ticket initializes it in OPEN status with initial history")
    void testCreateTicket() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(4);

        Ticket ticket = Ticket.createNew(
                1L,
                "user@helpflow.com",
                "Laptop not booting",
                "Screen stays black after power on",
                Category.HARDWARE,
                Priority.HIGH,
                "Headquarters",
                "LAPTOP-001",
                deadline,
                now
        );

        assertNotNull(ticket);
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(1L, ticket.getUserId());
        assertEquals("user@helpflow.com", ticket.getUserEmail());
        assertEquals(Category.HARDWARE, ticket.getCategory());
        assertEquals(Priority.HIGH, ticket.getPriority());
        assertEquals(1, ticket.getHistories().size());
        assertEquals(TicketStatus.OPEN, ticket.getHistories().get(0).getNewStatus());
        assertNull(ticket.getAssignedTechnicianId());
    }

    @Test
    @DisplayName("Assigning technician updates status to ASSIGNED and records assignment and history")
    void testAssignTechnician() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "VPN issue", "Cannot connect to VPN",
                Category.NETWORK, Priority.MEDIUM, "North Branch", "ROUTER-01",
                now.plusHours(8), now
        );

        ticket.assignTechnician(10L, "TEC-001", "admin@helpflow.com", "Assigned based on specialty", now);

        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());
        assertEquals(10L, ticket.getAssignedTechnicianId());
        assertEquals("TEC-001", ticket.getAssignedTechnicianCode());
        assertEquals(1, ticket.getAssignments().size());
        assertEquals("TEC-001", ticket.getAssignments().get(0).getTechnicianCode());
        assertEquals(2, ticket.getHistories().size()); // 1 initial + 1 assignment
        assertEquals(TicketStatus.ASSIGNED, ticket.getHistories().get(1).getNewStatus());
    }

    @Test
    @DisplayName("Valid ticket lifecycle transitions succeed")
    void testValidLifecycleTransitions() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "Password reset", "Locked out",
                Category.ACCESS, Priority.LOW, "HQ", "ACC-10",
                now.plusHours(24), now
        );

        // OPEN -> ASSIGNED
        ticket.assignTechnician(5L, "TEC-005", "admin@helpflow.com", "Assigned", now);
        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());

        // ASSIGNED -> IN_PROGRESS
        ticket.updateStatus(TicketStatus.IN_PROGRESS, "tech@helpflow.com", "Starting investigation", now);
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNotNull(ticket.getFirstResponseAt());

        // IN_PROGRESS -> RESOLVED
        ticket.updateStatus(TicketStatus.RESOLVED, "tech@helpflow.com", "Password reset link sent", now);
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertNotNull(ticket.getResolvedAt());

        // RESOLVED -> CLOSED
        ticket.updateStatus(TicketStatus.CLOSED, "user@helpflow.com", "Confirmed working, thanks", now);
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    @DisplayName("Invalid transition throws InvalidStateTransitionException")
    void testInvalidTransitions() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "Glitch", "System glitch",
                Category.SOFTWARE, Priority.LOW, "HQ", "APP-01",
                now.plusHours(24), now
        );

        // Cannot jump directly from OPEN to RESOLVED
        assertThrows(InvalidStateTransitionException.class, () ->
                ticket.updateStatus(TicketStatus.RESOLVED, "user@helpflow.com", "Testing invalid transition", now)
        );

        // Cannot move to IN_PROGRESS without assigned technician
        assertThrows(InvalidStateTransitionException.class, () ->
                ticket.updateStatus(TicketStatus.IN_PROGRESS, "user@helpflow.com", "No tech assigned", now)
        );
    }

    @Test
    @DisplayName("Updating status without observations throws BusinessRuleException")
    void testObservationsMandatory() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "Wifi down", "No signal",
                Category.NETWORK, Priority.CRITICAL, "HQ", "AP-01",
                now.plusHours(2), now
        );
        ticket.assignTechnician(2L, "TEC-002", "admin@helpflow.com", "Assigned", now);

        assertThrows(BusinessRuleException.class, () ->
                ticket.updateStatus(TicketStatus.IN_PROGRESS, "tech@helpflow.com", "", now)
        );
        assertThrows(BusinessRuleException.class, () ->
                ticket.updateStatus(TicketStatus.IN_PROGRESS, "tech@helpflow.com", null, now)
        );
    }

    @Test
    @DisplayName("Escalation updates priority and logs history")
    void testEscalation() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "Database lag", "Queries taking too long",
                Category.SOFTWARE, Priority.LOW, "HQ", "DB-01",
                now.plusHours(24), now
        );

        LocalDateTime newDeadline = now.plusHours(2);
        ticket.escalate(Priority.CRITICAL, newDeadline, "supervisor@helpflow.com", "SLA risk detected", now);

        assertEquals(Priority.CRITICAL, ticket.getPriority());
        assertEquals(newDeadline, ticket.getSlaDeadline());
        assertTrue(ticket.getHistories().stream().anyMatch(h -> h.getObservations().contains("escalated")));
    }

    @Test
    @DisplayName("Overdue ticket detection works correctly")
    void testOverdueDetection() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDeadline = now.minusMinutes(10);
        Ticket ticket = Ticket.createNew(
                1L, "user@helpflow.com", "Printer error", "Paper jam",
                Category.HARDWARE, Priority.MEDIUM, "HQ", "PRN-01",
                pastDeadline, now.minusHours(9)
        );

        assertTrue(ticket.isOverdue(now));

        // Resolved ticket is not overdue even if deadline was in past
        ticket.assignTechnician(1L, "TEC-001", "admin@helpflow.com", "Assigned", now.minusHours(5));
        ticket.updateStatus(TicketStatus.IN_PROGRESS, "tech@helpflow.com", "Working", now.minusHours(4));
        ticket.updateStatus(TicketStatus.RESOLVED, "tech@helpflow.com", "Fixed jam", now.minusHours(1));

        assertFalse(ticket.isOverdue(now));
    }
}
