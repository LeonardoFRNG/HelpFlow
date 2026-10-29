package com.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataTechnicianJpaRepository extends JpaRepository<TechnicianJpaEntity, Long> {
    Optional<TechnicianJpaEntity> findByCode(String code);

    Optional<TechnicianJpaEntity> findByDocument(String document);

    @Query("SELECT DISTINCT t FROM TechnicianJpaEntity t JOIN t.specialties s WHERE s = :category AND t.status = 'ACTIVE'")
    List<TechnicianJpaEntity> findActiveBySpecialty(@Param("category") Category category);
}
