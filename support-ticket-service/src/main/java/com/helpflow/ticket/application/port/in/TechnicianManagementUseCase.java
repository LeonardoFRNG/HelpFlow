package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;

import java.util.List;
import java.util.Set;

public interface TechnicianManagementUseCase {

    record CreateTechnicianCommand(
            String code,
            String document,
            String name,
            String email,
            String location,
            Set<Category> specialties,
            int maxActiveTickets
    ) {}

    Technician createTechnician(CreateTechnicianCommand command);

    Technician getTechnicianById(Long id);

    Technician getTechnicianByCode(String code);

    List<Technician> getAllTechnicians();

    Technician toggleTechnicianStatus(Long id, boolean active);
}
