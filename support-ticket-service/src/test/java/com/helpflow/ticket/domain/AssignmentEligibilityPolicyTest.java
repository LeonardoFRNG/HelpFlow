package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.exception.TechnicianNotEligibleException;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import com.helpflow.ticket.domain.policy.AssignmentEligibilityPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Assignment Eligibility Policy Tests")
class AssignmentEligibilityPolicyTest {

    private final AssignmentEligibilityPolicy policy = new AssignmentEligibilityPolicy();

    @Test
    @DisplayName("Active technician with specialty and capacity is eligible")
    void testEligibleTechnician() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-01", "Bob", "bob@helpflow.com",
                "HQ", Set.of(Category.NETWORK), TechnicianStatus.ACTIVE, 5
        );

        assertTrue(policy.isEligible(tech, Category.NETWORK, 2));
        assertDoesNotThrow(() -> policy.validateEligibility(tech, Category.NETWORK, 2));
    }

    @Test
    @DisplayName("Inactive technician throws TechnicianNotEligibleException")
    void testInactiveTechnicianThrows() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-01", "Bob", "bob@helpflow.com",
                "HQ", Set.of(Category.NETWORK), TechnicianStatus.INACTIVE, 5
        );

        assertFalse(policy.isEligible(tech, Category.NETWORK, 2));
        assertThrows(TechnicianNotEligibleException.class, () ->
                policy.validateEligibility(tech, Category.NETWORK, 2)
        );
    }

    @Test
    @DisplayName("Technician without matching specialty throws TechnicianNotEligibleException")
    void testMissingSpecialtyThrows() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-01", "Bob", "bob@helpflow.com",
                "HQ", Set.of(Category.HARDWARE), TechnicianStatus.ACTIVE, 5
        );

        assertFalse(policy.isEligible(tech, Category.SOFTWARE, 2));
        assertThrows(TechnicianNotEligibleException.class, () ->
                policy.validateEligibility(tech, Category.SOFTWARE, 2)
        );
    }

    @Test
    @DisplayName("Overloaded technician throws TechnicianNotEligibleException")
    void testOverloadedTechnicianThrows() {
        Technician tech = new Technician(
                1L, "TEC-001", "DOC-01", "Bob", "bob@helpflow.com",
                "HQ", Set.of(Category.HARDWARE), TechnicianStatus.ACTIVE, 3
        );

        assertFalse(policy.isEligible(tech, Category.HARDWARE, 3));
        assertThrows(TechnicianNotEligibleException.class, () ->
                policy.validateEligibility(tech, Category.HARDWARE, 3)
        );
    }
}
