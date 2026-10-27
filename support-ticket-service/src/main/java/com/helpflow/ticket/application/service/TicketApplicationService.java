package com.helpflow.ticket.application.service;

import com.helpflow.ticket.application.port.in.AssignTechnicianUseCase;
import com.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.helpflow.ticket.application.port.in.EscalateTicketUseCase;
import com.helpflow.ticket.application.port.in.GetTicketUseCase;
import com.helpflow.ticket.application.port.in.UpdateTicketStatusUseCase;
import com.helpflow.ticket.application.port.out.DispatchResult;
import com.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.TechnicianNotFoundException;
import com.helpflow.ticket.domain.exception.TicketNotFoundException;
import com.helpflow.ticket.domain.exception.UnauthorizedTicketAccessException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.exception.UserNotFoundException;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.policy.AssignmentEligibilityPolicy;
import com.helpflow.ticket.domain.policy.SlaPolicy;
import com.helpflow.ticket.infrastructure.observability.HelpFlowMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TicketApplicationService implements
        CreateTicketUseCase,
        GetTicketUseCase,
        UpdateTicketStatusUseCase,
        AssignTechnicianUseCase,
        EscalateTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(TicketApplicationService.class);

    private final TicketRepositoryPort ticketRepository;
    private final UserRepositoryPort userRepository;
    private final TechnicianRepositoryPort technicianRepository;
    private final TechnicianDispatchPort dispatchPort;
    private final SlaPolicy slaPolicy = new SlaPolicy();
    private final AssignmentEligibilityPolicy eligibilityPolicy = new AssignmentEligibilityPolicy();

    @Autowired(required = false)
    private HelpFlowMetrics metrics;

    public TicketApplicationService(TicketRepositoryPort ticketRepository,
                                  UserRepositoryPort userRepository,
                                  TechnicianRepositoryPort technicianRepository,
                                  TechnicianDispatchPort dispatchPort) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.dispatchPort = dispatchPort;
    }

    public void setMetrics(HelpFlowMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    @Transactional
    public Ticket createTicket(CreateTicketCommand command) {
        User user = userRepository.findByCorporateEmail(command.requesterEmail())
                .orElseThrow(() -> new UserNotFoundException(command.requesterEmail()));

        if (!user.isActive()) {
            throw new UserInactiveException(user.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadline = slaPolicy.calculateDeadline(command.priority(), now);

        Ticket ticket = Ticket.createNew(
                user.getId(),
                user.getCorporateEmail(),
                command.title(),
                command.description(),
                command.category(),
                command.priority(),
                command.location() != null ? command.location() : user.getLocation(),
                command.affectedAsset(),
                slaDeadline,
                now
        );

        // Save initial ticket in OPEN state
        ticket = ticketRepository.save(ticket);
        log.info("Ticket created with ID {} in OPEN state for user {}", ticket.getId(), user.getCorporateEmail());

        if (metrics != null) {
            metrics.recordTicketCreated(ticket.getCategory(), ticket.getPriority());
        }

        // Consult external technician dispatch service
        try {
            DispatchResult dispatchResult = dispatchPort.dispatch(ticket.getId(), ticket.getCategory(), ticket.getPriority());
            if (dispatchResult != null && dispatchResult.technicianId() != null) {
                String suggestedCode = dispatchResult.technicianId();
                Optional<Technician> techOpt = technicianRepository.findByCode(suggestedCode);

                if (techOpt.isPresent()) {
                    Technician tech = techOpt.get();
                    int activeCount = ticketRepository.countActiveTicketsByTechnicianId(tech.getId());

                    if (eligibilityPolicy.isEligible(tech, ticket.getCategory(), activeCount)) {
                        ticket.assignTechnician(
                                tech.getId(),
                                tech.getCode(),
                                "DISPATCH_AUTO",
                                "Auto-assigned by Technician Dispatch Service (" + dispatchResult.availability() + " availability)",
                                now
                        );
                        ticket = ticketRepository.save(ticket);
                        log.info("Ticket {} automatically assigned to technician {}", ticket.getId(), tech.getCode());
                    } else {
                        log.warn("Suggested technician {} is not eligible or overloaded for ticket {}", tech.getCode(), ticket.getId());
                        tryFallbackAssignment(ticket, now);
                    }
                } else {
                    log.warn("Technician with code {} from dispatch not found locally", suggestedCode);
                    tryFallbackAssignment(ticket, now);
                }
            }
        } catch (Exception ex) {
            log.error("Technician dispatch service failure for ticket {}: {}. Ticket will remain OPEN.",
                    ticket.getId(), ex.getMessage());
        }

        return ticket;
    }

    private void tryFallbackAssignment(Ticket ticket, LocalDateTime now) {
        List<Technician> candidates = technicianRepository.findActiveBySpecialty(ticket.getCategory());
        for (Technician tech : candidates) {
            int count = ticketRepository.countActiveTicketsByTechnicianId(tech.getId());
            if (tech.canAcceptMoreTickets(count)) {
                ticket.assignTechnician(
                        tech.getId(),
                        tech.getCode(),
                        "FALLBACK_DISPATCH",
                        "Assigned via internal specialty fallback pool",
                        now
                );
                ticketRepository.save(ticket);
                log.info("Ticket {} assigned via fallback pool to technician {}", ticket.getId(), tech.getCode());
                return;
            }
        }
        log.info("No available technician found in fallback pool for ticket {}. Remains in OPEN status.", ticket.getId());
    }

    @Override
    public Ticket getTicketById(Long id, String requesterEmail, Role requesterRole) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        validateAccess(ticket, requesterEmail, requesterRole);
        return ticket;
    }

    @Override
    public List<Ticket> getTickets(String requesterEmail, Role requesterRole, TicketFilter filter) {
        Long filterUserId = null;
        Long filterTechId = null;

        if (requesterRole == Role.ROLE_USUARIO) {
            User user = userRepository.findByCorporateEmail(requesterEmail)
                    .orElseThrow(() -> new UserNotFoundException(requesterEmail));
            filterUserId = user.getId();
        } else if (requesterRole == Role.ROLE_TECNICO) {
            Optional<Technician> techOpt = technicianRepository.findAll().stream()
                    .filter(t -> requesterEmail.equalsIgnoreCase(t.getEmail()))
                    .findFirst();
            if (techOpt.isPresent()) {
                filterTechId = techOpt.get().getId();
            }
        }

        return ticketRepository.findByFilters(
                filterUserId,
                filterTechId,
                filter != null ? filter.status() : null,
                filter != null ? filter.priority() : null,
                filter != null ? filter.category() : null
        );
    }

    @Override
    @Transactional
    public Ticket updateStatus(UpdateStatusCommand command) {
        Ticket ticket = ticketRepository.findById(command.ticketId())
                .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));

        validateAccess(ticket, command.updatedByEmail(), command.updatedByRole());

        // Technician can only update tickets assigned to them
        if (command.updatedByRole() == Role.ROLE_TECNICO) {
            Technician tech = technicianRepository.findAll().stream()
                    .filter(t -> command.updatedByEmail().equalsIgnoreCase(t.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Technician profile not found for " + command.updatedByEmail()));

            if (!tech.getId().equals(ticket.getAssignedTechnicianId())) {
                throw new UnauthorizedTicketAccessException("Technician is not assigned to ticket " + ticket.getId());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.updateStatus(command.targetStatus(), command.updatedByEmail(), command.observations(), now);
        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket {} status transitioned to {} by {}", updated.getId(), updated.getStatus(), command.updatedByEmail());

        if (metrics != null) {
            if (command.targetStatus() == TicketStatus.IN_PROGRESS && updated.getFirstResponseAt() != null && updated.getCreatedAt() != null) {
                metrics.recordFirstResponseTime(Duration.between(updated.getCreatedAt(), updated.getFirstResponseAt()));
            } else if (command.targetStatus() == TicketStatus.RESOLVED && updated.getResolvedAt() != null && updated.getCreatedAt() != null) {
                metrics.recordResolutionTime(Duration.between(updated.getCreatedAt(), updated.getResolvedAt()));
            }
        }

        return updated;
    }

    @Override
    @Transactional
    public Ticket assignTechnician(AssignTechnicianCommand command) {
        Ticket ticket = ticketRepository.findById(command.ticketId())
                .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));

        Technician tech = technicianRepository.findByCode(command.technicianCode())
                .orElseThrow(() -> new TechnicianNotFoundException(command.technicianCode()));

        int activeTickets = ticketRepository.countActiveTicketsByTechnicianId(tech.getId());
        eligibilityPolicy.validateEligibility(tech, ticket.getCategory(), activeTickets);

        LocalDateTime now = LocalDateTime.now();
        ticket.assignTechnician(tech.getId(), tech.getCode(), command.assignedByEmail(), command.notes(), now);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} assigned to technician {} by admin {}", saved.getId(), tech.getCode(), command.assignedByEmail());
        return saved;
    }

    @Override
    @Transactional
    public Ticket escalateTicket(EscalateTicketCommand command) {
        Ticket ticket = ticketRepository.findById(command.ticketId())
                .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newDeadline = slaPolicy.calculateDeadline(command.targetPriority(), now);

        ticket.escalate(command.targetPriority(), newDeadline, command.escalatedByEmail(), command.reason(), now);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} escalated to priority {} by {}", saved.getId(), command.targetPriority(), command.escalatedByEmail());
        return saved;
    }

    @Override
    @Transactional
    public int checkAndEscalateApproachingBreachTickets(long thresholdMinutes) {
        List<Ticket> openTickets = ticketRepository.findOpenAndAssignedTickets();
        LocalDateTime now = LocalDateTime.now();
        int escalatedCount = 0;

        for (Ticket ticket : openTickets) {
            if (ticket.isApproachingBreach(now, thresholdMinutes)) {
                Priority nextPriority = getNextHigherPriority(ticket.getPriority());
                if (nextPriority != ticket.getPriority()) {
                    LocalDateTime newDeadline = slaPolicy.calculateDeadline(nextPriority, now);
                    ticket.escalate(nextPriority, newDeadline, "SYSTEM_SLA_MONITOR",
                            "Automatic escalation: ticket was within " + thresholdMinutes + " minutes of SLA deadline", now);
                    ticketRepository.save(ticket);
                    escalatedCount++;
                    log.warn("SLA Escalation: Ticket {} escalated to {} due to impending breach", ticket.getId(), nextPriority);
                }
            }
        }
        return escalatedCount;
    }

    private Priority getNextHigherPriority(Priority current) {
        return switch (current) {
            case LOW -> Priority.MEDIUM;
            case MEDIUM -> Priority.HIGH;
            case HIGH, CRITICAL -> Priority.CRITICAL;
        };
    }

    private void validateAccess(Ticket ticket, String email, Role role) {
        if (role == Role.ROLE_ADMIN) {
            return;
        }
        if (role == Role.ROLE_USUARIO) {
            if (!email.equalsIgnoreCase(ticket.getUserEmail())) {
                throw new UnauthorizedTicketAccessException(ticket.getId(), email);
            }
            return;
        }
        if (role == Role.ROLE_TECNICO) {
            Optional<Technician> techOpt = technicianRepository.findAll().stream()
                    .filter(t -> email.equalsIgnoreCase(t.getEmail()))
                    .findFirst();
            if (techOpt.isEmpty() || !techOpt.get().getId().equals(ticket.getAssignedTechnicianId())) {
                throw new UnauthorizedTicketAccessException(ticket.getId(), email);
            }
        }
    }
}
