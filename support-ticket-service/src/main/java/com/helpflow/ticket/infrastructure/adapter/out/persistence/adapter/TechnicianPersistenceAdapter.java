package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.TechnicianPersistenceMapper;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataTechnicianJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TechnicianPersistenceAdapter implements TechnicianRepositoryPort {

    private final SpringDataTechnicianJpaRepository repository;
    private final TechnicianPersistenceMapper mapper;

    public TechnicianPersistenceAdapter(SpringDataTechnicianJpaRepository repository, TechnicianPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Technician save(Technician technician) {
        TechnicianJpaEntity entity = mapper.toEntity(technician);
        TechnicianJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Technician> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Technician> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public Optional<Technician> findByDocument(String document) {
        return repository.findByDocument(document).map(mapper::toDomain);
    }

    @Override
    public List<Technician> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Technician> findActiveBySpecialty(Category category) {
        return repository.findActiveBySpecialty(category).stream().map(mapper::toDomain).toList();
    }
}
