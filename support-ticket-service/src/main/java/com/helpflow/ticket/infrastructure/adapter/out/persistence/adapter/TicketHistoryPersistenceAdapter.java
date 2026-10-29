package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.ticket.domain.model.TicketHistory;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketHistoryJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.TicketHistoryPersistenceMapper;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataTicketHistoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketHistoryPersistenceAdapter implements TicketHistoryRepositoryPort {

    private final SpringDataTicketHistoryJpaRepository repository;
    private final TicketHistoryPersistenceMapper mapper;

    public TicketHistoryPersistenceAdapter(SpringDataTicketHistoryJpaRepository repository, TicketHistoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TicketHistory save(TicketHistory history) {
        TicketHistoryJpaEntity entity = mapper.toEntity(history);
        TicketHistoryJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<TicketHistory> findByTicketId(Long ticketId) {
        return repository.findByTicketIdOrderByChangedAtAsc(ticketId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
