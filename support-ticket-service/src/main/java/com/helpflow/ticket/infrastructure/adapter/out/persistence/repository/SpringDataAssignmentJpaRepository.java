package com.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.AssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataAssignmentJpaRepository extends JpaRepository<AssignmentJpaEntity, Long> {
    @Query("SELECT a FROM AssignmentJpaEntity a WHERE a.ticket.id = :ticketId ORDER BY a.assignedAt ASC")
    List<AssignmentJpaEntity> findByTicketIdOrderByAssignedAtAsc(@Param("ticketId") Long ticketId);
}
