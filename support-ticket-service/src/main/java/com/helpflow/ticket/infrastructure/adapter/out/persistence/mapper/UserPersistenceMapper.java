package com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {
    UserJpaEntity toEntity(User domain);

    User toDomain(UserJpaEntity entity);
}
