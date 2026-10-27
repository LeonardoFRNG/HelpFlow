package com.helpflow.ticket.domain.policy;

import com.helpflow.ticket.domain.exception.TechnicianNotEligibleException;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;

public class AssignmentEligibilityPolicy {

    public void validateEligibility(Technician technician, Category category, int currentActiveTickets) {
        if (technician == null) {
            throw new TechnicianNotEligibleException("Technician cannot be null");
        }
        if (!technician.isActive()) {
            throw new TechnicianNotEligibleException(
                    String.format("Technician %s (%s) is INACTIVE", technician.getCode(), technician.getName()));
        }
        if (!technician.hasSpecialty(category)) {
            throw new TechnicianNotEligibleException(
                    String.format("Technician %s does not have specialty for category %s. Current specialties: %s",
                            technician.getCode(), category, technician.getSpecialties()));
        }
        if (!technician.canAcceptMoreTickets(currentActiveTickets)) {
            throw new TechnicianNotEligibleException(
                    String.format("Technician %s has reached the maximum workload limit (%d/%d active tickets)",
                            technician.getCode(), currentActiveTickets, technician.getMaxActiveTickets()));
        }
    }

    public boolean isEligible(Technician technician, Category category, int currentActiveTickets) {
        if (technician == null || !technician.isActive()) {
            return false;
        }
        if (!technician.hasSpecialty(category)) {
            return false;
        }
        return technician.canAcceptMoreTickets(currentActiveTickets);
    }
}
