package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;

import java.util.Set;

public record TechnicianResponseDto(
        Long id,
        String code,
        String document,
        String name,
        String email,
        String location,
        TechnicianStatus status,
        int maxActiveTickets,
        Set<Category> specialties
) {
    public static TechnicianResponseDto fromDomain(Technician technician) {
        return new TechnicianResponseDto(
                technician.getId(),
                technician.getCode(),
                technician.getDocument(),
                technician.getName(),
                technician.getEmail(),
                technician.getLocation(),
                technician.getStatus(),
                technician.getMaxActiveTickets(),
                technician.getSpecialties()
        );
    }
}
