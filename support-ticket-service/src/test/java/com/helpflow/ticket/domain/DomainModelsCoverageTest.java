package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.model.Availability;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Models and Enums Comprehensive Coverage Tests")
class DomainModelsCoverageTest {

    @Test
    @DisplayName("Category fromString aliases work correctly")
    void testCategoryAliases() {
        assertEquals(Category.NETWORK, Category.fromString("RED"));
        assertEquals(Category.ACCESS, Category.fromString("ACCESOS"));
        assertEquals(Category.OTHER, Category.fromString("OTRO"));
        assertEquals(Category.HARDWARE, Category.fromString("HARDWARE"));
        assertEquals(Category.SOFTWARE, Category.fromString("SOFTWARE"));
        assertThrows(IllegalArgumentException.class, () -> Category.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Category.fromString(null));
    }

    @Test
    @DisplayName("Priority fromString aliases work correctly")
    void testPriorityAliases() {
        assertEquals(Priority.LOW, Priority.fromString("BAJA"));
        assertEquals(Priority.MEDIUM, Priority.fromString("MEDIA"));
        assertEquals(Priority.HIGH, Priority.fromString("ALTA"));
        assertEquals(Priority.CRITICAL, Priority.fromString("CRITICA"));
        assertEquals(Priority.CRITICAL, Priority.fromString("CRÍTICA"));
        assertThrows(IllegalArgumentException.class, () -> Priority.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Priority.fromString(null));
    }

    @Test
    @DisplayName("TicketStatus fromString aliases work correctly")
    void testTicketStatusAliases() {
        assertEquals(TicketStatus.OPEN, TicketStatus.fromString("ABIERTO"));
        assertEquals(TicketStatus.ASSIGNED, TicketStatus.fromString("ASIGNADO"));
        assertEquals(TicketStatus.IN_PROGRESS, TicketStatus.fromString("EN_PROGRESO"));
        assertEquals(TicketStatus.RESOLVED, TicketStatus.fromString("RESUELTO"));
        assertEquals(TicketStatus.CLOSED, TicketStatus.fromString("CERRADO"));
        assertEquals(TicketStatus.CANCELLED, TicketStatus.fromString("CANCELADO"));
        assertThrows(IllegalArgumentException.class, () -> TicketStatus.fromString(""));
    }

    @Test
    @DisplayName("Role and UserStatus fromString work correctly")
    void testRoleAndStatus() {
        assertEquals(Role.ROLE_ADMIN, Role.fromString("ADMIN"));
        assertEquals(Role.ROLE_TECNICO, Role.fromString("TECNICO"));
        assertEquals(Role.ROLE_USUARIO, Role.fromString("USUARIO"));
        assertEquals(Role.ROLE_USUARIO, Role.fromString(null));

        assertEquals(UserStatus.ACTIVE, UserStatus.fromString("ACTIVO"));
        assertEquals(UserStatus.INACTIVE, UserStatus.fromString("INACTIVO"));
        assertEquals(UserStatus.ACTIVE, UserStatus.fromString(null));

        assertEquals(TechnicianStatus.ACTIVE, TechnicianStatus.fromString("ACTIVO"));
        assertEquals(TechnicianStatus.INACTIVE, TechnicianStatus.fromString("INACTIVO"));
        assertEquals(TechnicianStatus.ACTIVE, TechnicianStatus.fromString(null));

        assertEquals(Availability.HIGH, Availability.fromString("ALTA"));
        assertEquals(Availability.MEDIUM, Availability.fromString("MEDIA"));
        assertEquals(Availability.LOW, Availability.fromString("BAJA"));
        assertEquals(Availability.MEDIUM, Availability.fromString(null));
    }

    @Test
    @DisplayName("User and Technician getters, setters and equality")
    void testUserAndTechDetails() {
        User user = new User();
        user.setId(10L);
        user.setDocument("DOC-10");
        user.setName("Name");
        user.setCorporateEmail("name@helpflow.com");
        user.setPassword("pass");
        user.setLocation("HQ");
        user.setArea("IT");
        user.setRole(Role.ROLE_USUARIO);
        user.setStatus(UserStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("DOC-10", user.getDocument());
        assertEquals("Name", user.getName());
        assertEquals("name@helpflow.com", user.getCorporateEmail());
        assertEquals("pass", user.getPassword());
        assertEquals("HQ", user.getLocation());
        assertEquals("IT", user.getArea());
        assertEquals(Role.ROLE_USUARIO, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(now, user.getCreatedAt());

        Technician tech = new Technician();
        tech.setId(20L);
        tech.setCode("TEC-020");
        tech.setDocument("DOC-T20");
        tech.setName("Tech Twenty");
        tech.setEmail("tech20@helpflow.com");
        tech.setLocation("Sede Sur");
        tech.setStatus(TechnicianStatus.ACTIVE);
        tech.setMaxActiveTickets(5);
        tech.addSpecialty(Category.HARDWARE);
        tech.removeSpecialty(Category.NETWORK);

        assertEquals("TEC-020", tech.getCode());
        assertTrue(tech.hasSpecialty(Category.HARDWARE));
        assertEquals(20L, tech.getId());
        assertEquals("DOC-T20", tech.getDocument());
        assertEquals("Tech Twenty", tech.getName());
        assertEquals("tech20@helpflow.com", tech.getEmail());
        assertEquals("Sede Sur", tech.getLocation());
        assertEquals(TechnicianStatus.ACTIVE, tech.getStatus());
        assertEquals(5, tech.getMaxActiveTickets());
    }

    @Test
    @DisplayName("Ticket getters and setters")
    void testTicketGettersSetters() {
        Ticket ticket = new Ticket();
        ticket.setId(55L);
        ticket.setDescription("Sample Description");
        ticket.setLocation("North");
        ticket.setAffectedAsset("PRN-10");

        assertEquals(55L, ticket.getId());
        assertEquals("Sample Description", ticket.getDescription());
        assertEquals("North", ticket.getLocation());
        assertEquals("PRN-10", ticket.getAffectedAsset());
        assertNotNull(ticket.hashCode());
        assertTrue(ticket.equals(ticket));
        assertFalse(ticket.equals(null));
    }
}
