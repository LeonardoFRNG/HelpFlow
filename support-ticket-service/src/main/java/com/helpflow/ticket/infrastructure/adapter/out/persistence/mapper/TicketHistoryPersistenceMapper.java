package com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.ticket.domain.model.TicketHistory;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketHistoryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketHistoryPersistenceMapper {
    @Mapping(target = "ticket", ignore = true)
    TicketHistoryJpaEntity toEntity(TicketHistory domain);

    @Mapping(target = "ticketId", source = "ticket.id")
    TicketHistory toDomain(TicketHistoryJpaEntity entity);

    List<TicketHistoryJpaEntity> toEntityList(List<TicketHistory> domainList);

    List<TicketHistory> toDomainList(List<TicketHistoryJpaEntity> entityList);
}
