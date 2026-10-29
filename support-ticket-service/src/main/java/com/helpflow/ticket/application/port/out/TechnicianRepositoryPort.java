package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;

import java.util.List;
import java.util.Optional;

public interface TechnicianRepositoryPort {
    Technician save(Technician technician);

    Optional<Technician> findById(Long id);

    Optional<Technician> findByCode(String code);

    Optional<Technician> findByDocument(String document);

    List<Technician> findAll();

    List<Technician> findActiveBySpecialty(Category category);
}
