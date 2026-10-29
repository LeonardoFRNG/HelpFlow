package com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AssignmentPersistenceMapper.class, TicketHistoryPersistenceMapper.class})
public interface TicketPersistenceMapper {
    TicketJpaEntity toEntity(Ticket domain);

    Ticket toDomain(TicketJpaEntity entity);

    List<Ticket> toDomainList(List<TicketJpaEntity> entityList);
}
