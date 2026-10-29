package com.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataTicketJpaRepository extends JpaRepository<TicketJpaEntity, Long> {

    @EntityGraph(attributePaths = {"assignments", "histories"})
    @Query("SELECT t FROM TicketJpaEntity t WHERE t.id = :id")
    Optional<TicketJpaEntity> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"assignments", "histories"})
    @Query("SELECT t FROM TicketJpaEntity t")
    List<TicketJpaEntity> findAllWithDetails();

    @EntityGraph(attributePaths = {"assignments", "histories"})
    List<TicketJpaEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"assignments", "histories"})
    List<TicketJpaEntity> findByAssignedTechnicianId(Long assignedTechnicianId);

    @Query("SELECT COUNT(t) FROM TicketJpaEntity t WHERE t.assignedTechnicianId = :techId AND t.status IN ('ASSIGNED', 'IN_PROGRESS')")
    int countActiveTicketsByTechnicianId(@Param("techId") Long techId);

    @EntityGraph(attributePaths = {"assignments", "histories"})
    @Query("SELECT t FROM TicketJpaEntity t WHERE t.status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')")
    List<TicketJpaEntity> findOpenAndAssignedTickets();

    @Query("SELECT t FROM TicketJpaEntity t WHERE t.slaDeadline < :now AND t.status NOT IN ('RESOLVED', 'CLOSED', 'CANCELLED')")
    List<TicketJpaEntity> findOverdueTickets(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM TicketJpaEntity t WHERE " +
            "(:userId IS NULL OR t.userId = :userId) AND " +
            "(:techId IS NULL OR t.assignedTechnicianId = :techId) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:priority IS NULL OR t.priority = :priority) AND " +
            "(:category IS NULL OR t.category = :category) " +
            "ORDER BY t.createdAt DESC")
    List<TicketJpaEntity> findByFilters(
            @Param("userId") Long userId,
            @Param("techId") Long techId,
            @Param("status") TicketStatus status,
            @Param("priority") Priority priority,
            @Param("category") Category category
    );
}
