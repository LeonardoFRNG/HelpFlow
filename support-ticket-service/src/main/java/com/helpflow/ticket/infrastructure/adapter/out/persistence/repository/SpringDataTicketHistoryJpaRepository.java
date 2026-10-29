package com.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTicketHistoryJpaRepository extends JpaRepository<TicketHistoryJpaEntity, Long> {
    @Query("SELECT h FROM TicketHistoryJpaEntity h WHERE h.ticket.id = :ticketId ORDER BY h.changedAt ASC")
    List<TicketHistoryJpaEntity> findByTicketIdOrderByChangedAtAsc(@Param("ticketId") Long ticketId);
}
