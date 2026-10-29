package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Availability;

public record DispatchResult(
        Long ticketId,
        String technicianId,
        Availability availability,
        int estimatedHours
) {}
