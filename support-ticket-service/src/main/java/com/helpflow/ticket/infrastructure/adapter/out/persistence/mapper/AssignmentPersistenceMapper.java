package com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.ticket.domain.model.Assignment;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.AssignmentJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssignmentPersistenceMapper {
    @Mapping(target = "ticket", ignore = true)
    AssignmentJpaEntity toEntity(Assignment domain);

    @Mapping(target = "ticketId", source = "ticket.id")
    Assignment toDomain(AssignmentJpaEntity entity);

    List<AssignmentJpaEntity> toEntityList(List<Assignment> domainList);

    List<Assignment> toDomainList(List<AssignmentJpaEntity> entityList);
}
