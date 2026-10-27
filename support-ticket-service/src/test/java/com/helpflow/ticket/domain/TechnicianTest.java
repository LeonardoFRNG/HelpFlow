package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Technician Domain Entity Tests")
class TechnicianTest {

    @Test
    @DisplayName("Technician specialties and eligibility verification")
    void testTechnicianSpecialty() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-T1", "John Tech", "john@helpflow.com",
                "HQ", Set.of(Category.NETWORK, Category.HARDWARE), TechnicianStatus.ACTIVE, 5
        );

        assertTrue(tech.hasSpecialty(Category.NETWORK));
        assertTrue(tech.hasSpecialty(Category.HARDWARE));
        assertFalse(tech.hasSpecialty(Category.SOFTWARE));
        assertTrue(tech.isEligibleFor(Category.NETWORK));
    }

    @Test
    @DisplayName("Inactive technician is not eligible even with specialty")
    void testInactiveTechnicianNotEligible() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-T1", "John Tech", "john@helpflow.com",
                "HQ", Set.of(Category.NETWORK), TechnicianStatus.INACTIVE, 5
        );

        assertTrue(tech.hasSpecialty(Category.NETWORK));
        assertFalse(tech.isEligibleFor(Category.NETWORK));
    }

    @Test
    @DisplayName("Technician workload limits")
    void testWorkloadLimit() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-T1", "John Tech", "john@helpflow.com",
                "HQ", Set.of(Category.NETWORK), TechnicianStatus.ACTIVE, 3
        );

        assertTrue(tech.canAcceptMoreTickets(0));
        assertTrue(tech.canAcceptMoreTickets(2));
        assertFalse(tech.canAcceptMoreTickets(3));
        assertFalse(tech.canAcceptMoreTickets(4));
    }
}
