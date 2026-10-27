package com.helpflow.ticket.application.service;

import com.helpflow.ticket.application.port.in.TechnicianManagementUseCase;
import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.TechnicianNotFoundException;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TechnicianApplicationService implements TechnicianManagementUseCase {

    private final TechnicianRepositoryPort technicianRepository;

    public TechnicianApplicationService(TechnicianRepositoryPort technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    @Override
    @Transactional
    public Technician createTechnician(CreateTechnicianCommand command) {
        if (technicianRepository.findByCode(command.code()).isPresent()) {
            throw new BusinessRuleException("Technician with code " + command.code() + " already exists");
        }
        if (technicianRepository.findByDocument(command.document()).isPresent()) {
            throw new BusinessRuleException("Technician with document " + command.document() + " already exists");
        }

        Technician technician = new Technician(
                null,
                command.code(),
                command.document(),
                command.name(),
                command.email(),
                command.location(),
                command.specialties(),
                TechnicianStatus.ACTIVE,
                command.maxActiveTickets() > 0 ? command.maxActiveTickets() : 5
        );

        return technicianRepository.save(technician);
    }

    @Override
    public Technician getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new TechnicianNotFoundException(id));
    }

    @Override
    public Technician getTechnicianByCode(String code) {
        return technicianRepository.findByCode(code)
                .orElseThrow(() -> new TechnicianNotFoundException(code));
    }

    @Override
    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    @Override
    @Transactional
    public Technician toggleTechnicianStatus(Long id, boolean active) {
        Technician technician = getTechnicianById(id);
        if (active) {
            technician.activate();
        } else {
            technician.deactivate();
        }
        return technicianRepository.save(technician);
    }
}
