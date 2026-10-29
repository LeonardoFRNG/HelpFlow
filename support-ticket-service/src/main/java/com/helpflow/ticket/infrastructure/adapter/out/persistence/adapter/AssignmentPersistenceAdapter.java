package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.AssignmentRepositoryPort;
import com.helpflow.ticket.domain.model.Assignment;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.AssignmentJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.AssignmentPersistenceMapper;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataAssignmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssignmentPersistenceAdapter implements AssignmentRepositoryPort {

    private final SpringDataAssignmentJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public AssignmentPersistenceAdapter(SpringDataAssignmentJpaRepository repository, AssignmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Assignment save(Assignment assignment) {
        AssignmentJpaEntity entity = mapper.toEntity(assignment);
        AssignmentJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Assignment> findByTicketId(Long ticketId) {
        return repository.findByTicketIdOrderByAssignedAtAsc(ticketId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
