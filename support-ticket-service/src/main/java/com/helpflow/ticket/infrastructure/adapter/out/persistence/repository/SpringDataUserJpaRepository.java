package com.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByCorporateEmail(String corporateEmail);

    Optional<UserJpaEntity> findByDocument(String document);

    boolean existsByCorporateEmail(String corporateEmail);

    boolean existsByDocument(String document);
}
