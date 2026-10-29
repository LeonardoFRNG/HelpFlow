package com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TechnicianPersistenceMapper {
    TechnicianJpaEntity toEntity(Technician domain);

    Technician toDomain(TechnicianJpaEntity entity);
}
