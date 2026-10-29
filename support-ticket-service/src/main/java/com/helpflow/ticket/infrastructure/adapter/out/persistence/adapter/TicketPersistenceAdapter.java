package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataTicketJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class TicketPersistenceAdapter implements TicketRepositoryPort {

    private final SpringDataTicketJpaRepository repository;
    private final TicketPersistenceMapper mapper;

    public TicketPersistenceAdapter(SpringDataTicketJpaRepository repository, TicketPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketJpaEntity entity = mapper.toEntity(ticket);

        if (entity.getAssignments() != null) {
            entity.getAssignments().forEach(a -> a.setTicket(entity));
        }
        if (entity.getHistories() != null) {
            entity.getHistories().forEach(h -> h.setTicket(entity));
        }

        TicketJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return repository.findWithDetailsById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return repository.findAllWithDetails().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByAssignedTechnicianId(Long technicianId) {
        return repository.findByAssignedTechnicianId(technicianId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByFilters(Long userId, Long technicianId, TicketStatus status, Priority priority, Category category) {
        return repository.findByFilters(userId, technicianId, status, priority, category).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countActiveTicketsByTechnicianId(Long technicianId) {
        return repository.countActiveTicketsByTechnicianId(technicianId);
    }

    @Override
    public List<Ticket> findOpenAndAssignedTickets() {
        return repository.findOpenAndAssignedTickets().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findOverdueTickets(LocalDateTime now) {
        return repository.findOverdueTickets(now).stream().map(mapper::toDomain).toList();
    }
}
