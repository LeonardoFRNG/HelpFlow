package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.AssignTechnicianUseCase;
import com.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.helpflow.ticket.application.port.in.EscalateTicketUseCase;
import com.helpflow.ticket.application.port.in.GetTicketUseCase;
import com.helpflow.ticket.application.port.in.UpdateTicketStatusUseCase;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.AssignTechnicianRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.CreateTicketRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.EscalateTicketRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.TicketResponseDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.UpdateStatusRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "IT Support Ticket management and lifecycle operations")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final CreateTicketUseCase createTicketUseCase;
    private final GetTicketUseCase getTicketUseCase;
    private final UpdateTicketStatusUseCase updateTicketStatusUseCase;
    private final AssignTechnicianUseCase assignTechnicianUseCase;
    private final EscalateTicketUseCase escalateTicketUseCase;

    public TicketController(CreateTicketUseCase createTicketUseCase,
                            GetTicketUseCase getTicketUseCase,
                            UpdateTicketStatusUseCase updateTicketStatusUseCase,
                            AssignTechnicianUseCase assignTechnicianUseCase,
                            EscalateTicketUseCase escalateTicketUseCase) {
        this.createTicketUseCase = createTicketUseCase;
        this.getTicketUseCase = getTicketUseCase;
        this.updateTicketStatusUseCase = updateTicketStatusUseCase;
        this.assignTechnicianUseCase = assignTechnicianUseCase;
        this.escalateTicketUseCase = escalateTicketUseCase;
    }

    @PostMapping
    @Operation(summary = "Create support ticket", description = "Creates a ticket, calculates SLA deadline, and automatically dispatches to an eligible technician")
    public ResponseEntity<TicketResponseDto> createTicket(@Valid @RequestBody CreateTicketRequestDto request,
                                                         Principal principal) {
        Category category = Category.fromString(request.category());
        Priority priority = Priority.fromString(request.priority());

        CreateTicketUseCase.CreateTicketCommand command = new CreateTicketUseCase.CreateTicketCommand(
                principal.getName(),
                request.title(),
                request.description(),
                category,
                priority,
                request.location(),
                request.affectedAsset()
        );

        Ticket created = createTicketUseCase.createTicket(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponseDto.fromDomain(created));
    }

    @GetMapping
    @Operation(summary = "List tickets", description = "Retrieves tickets filtered by ownership, role, and optional parameters")
    public ResponseEntity<List<TicketResponseDto>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            Authentication authentication) {

        Role role = extractRole(authentication);
        TicketStatus filterStatus = status != null ? TicketStatus.fromString(status) : null;
        Priority filterPriority = priority != null ? Priority.fromString(priority) : null;
        Category filterCategory = category != null ? Category.fromString(category) : null;

        GetTicketUseCase.TicketFilter filter = new GetTicketUseCase.TicketFilter(
                filterStatus, filterPriority, filterCategory
        );

        List<Ticket> tickets = getTicketUseCase.getTickets(authentication.getName(), role, filter);
        List<TicketResponseDto> response = tickets.stream().map(TicketResponseDto::fromDomain).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details", description = "Retrieves complete ticket aggregate with assignments and history")
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable Long id, Authentication authentication) {
        Role role = extractRole(authentication);
        Ticket ticket = getTicketUseCase.getTicketById(id, authentication.getName(), role);
        return ResponseEntity.ok(TicketResponseDto.fromDomain(ticket));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ticket status", description = "Transitions ticket status according to state machine rules with observations")
    public ResponseEntity<TicketResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequestDto request,
            Authentication authentication) {

        Role role = extractRole(authentication);
        TicketStatus targetStatus = TicketStatus.fromString(request.status());

        UpdateTicketStatusUseCase.UpdateStatusCommand command = new UpdateTicketStatusUseCase.UpdateStatusCommand(
                id,
                targetStatus,
                request.observations(),
                authentication.getName(),
                role
        );

        Ticket updated = updateTicketStatusUseCase.updateStatus(command);
        return ResponseEntity.ok(TicketResponseDto.fromDomain(updated));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Assign or reassign technician", description = "Admin manual technician assignment with specialty and capacity validation")
    public ResponseEntity<TicketResponseDto> assignTechnician(
            @PathVariable Long id,
            @Valid @RequestBody AssignTechnicianRequestDto request,
            Principal principal) {

        AssignTechnicianUseCase.AssignTechnicianCommand command = new AssignTechnicianUseCase.AssignTechnicianCommand(
                id,
                request.technicianCode(),
                principal.getName(),
                request.notes()
        );

        Ticket assigned = assignTechnicianUseCase.assignTechnician(command);
        return ResponseEntity.ok(TicketResponseDto.fromDomain(assigned));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Escalate ticket priority", description = "Admin escalation to higher priority with recalculated SLA deadline")
    public ResponseEntity<TicketResponseDto> escalateTicket(
            @PathVariable Long id,
            @Valid @RequestBody EscalateTicketRequestDto request,
            Principal principal) {

        Priority targetPriority = Priority.fromString(request.targetPriority());

        EscalateTicketUseCase.EscalateTicketCommand command = new EscalateTicketUseCase.EscalateTicketCommand(
                id,
                targetPriority,
                request.reason(),
                principal.getName()
        );

        Ticket escalated = escalateTicketUseCase.escalateTicket(command);
        return ResponseEntity.ok(TicketResponseDto.fromDomain(escalated));
    }

    private Role extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            return Role.ROLE_USUARIO;
        }
        String authName = authentication.getAuthorities().iterator().next().getAuthority();
        return Role.fromString(authName);
    }
}
