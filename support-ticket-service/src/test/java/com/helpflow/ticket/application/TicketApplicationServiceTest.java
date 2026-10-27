package com.helpflow.ticket.application;

import com.helpflow.ticket.application.port.in.AssignTechnicianUseCase;
import com.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.helpflow.ticket.application.port.in.EscalateTicketUseCase;
import com.helpflow.ticket.application.port.in.UpdateTicketStatusUseCase;
import com.helpflow.ticket.application.port.out.DispatchResult;
import com.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.application.service.TicketApplicationService;
import com.helpflow.ticket.domain.exception.UnauthorizedTicketAccessException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.model.Availability;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ticket Application Service Unit Tests")
class TicketApplicationServiceTest {

    @Mock
    private TicketRepositoryPort ticketRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TechnicianRepositoryPort technicianRepository;

    @Mock
    private TechnicianDispatchPort dispatchPort;

    @InjectMocks
    private TicketApplicationService ticketService;

    private User activeUser;
    private Technician activeTechnician;

    @BeforeEach
    void setUp() {
        activeUser = new User(1L, "DOC-U1", "Juan Perez", "juan.perez@helpflow.com", "pass",
                "Sede Principal", "Ventas", Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now());

        activeTechnician = new Technician(10L, "TEC-001", "DOC-T1", "Tech One", "tech1@helpflow.com",
                "Sede Principal", Set.of(Category.HARDWARE, Category.NETWORK), TechnicianStatus.ACTIVE, 5);
    }

    @Test
    @DisplayName("Create ticket successfully calls dispatch and assigns eligible technician")
    void testCreateTicketSuccess() {
        when(userRepository.findByCorporateEmail(activeUser.getCorporateEmail())).thenReturn(Optional.of(activeUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(100L);
            }
            return t;
        });

        DispatchResult dispatchResult = new DispatchResult(100L, "TEC-001", Availability.HIGH, 2);
        when(dispatchPort.dispatch(eq(100L), eq(Category.HARDWARE), eq(Priority.HIGH))).thenReturn(dispatchResult);
        when(technicianRepository.findByCode("TEC-001")).thenReturn(Optional.of(activeTechnician));
        when(ticketRepository.countActiveTicketsByTechnicianId(10L)).thenReturn(2);

        CreateTicketUseCase.CreateTicketCommand cmd = new CreateTicketUseCase.CreateTicketCommand(
                activeUser.getCorporateEmail(),
                "Broken Monitor",
                "Display flickers violently",
                Category.HARDWARE,
                Priority.HIGH,
                "Sede Principal",
                "MON-09"
        );

        Ticket result = ticketService.createTicket(cmd);

        assertNotNull(result);
        assertEquals(TicketStatus.ASSIGNED, result.getStatus());
        assertEquals(10L, result.getAssignedTechnicianId());
        assertEquals("TEC-001", result.getAssignedTechnicianCode());
        verify(dispatchPort).dispatch(eq(100L), eq(Category.HARDWARE), eq(Priority.HIGH));
        verify(ticketRepository, atLeast(2)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Create ticket throws UserInactiveException when user is inactive")
    void testCreateTicketInactiveUser() {
        activeUser.deactivate();
        when(userRepository.findByCorporateEmail(activeUser.getCorporateEmail())).thenReturn(Optional.of(activeUser));

        CreateTicketUseCase.CreateTicketCommand cmd = new CreateTicketUseCase.CreateTicketCommand(
                activeUser.getCorporateEmail(), "Title", "Desc", Category.HARDWARE, Priority.LOW, "HQ", "ASSET-1"
        );

        assertThrows(UserInactiveException.class, () -> ticketService.createTicket(cmd));
        verifyNoInteractions(dispatchPort);
    }

    @Test
    @DisplayName("Updating ticket status of another user throws UnauthorizedTicketAccessException")
    void testUnauthorizedTicketAccess() {
        Ticket ticket = Ticket.createNew(
                2L, "other@helpflow.com", "Title", "Desc",
                Category.SOFTWARE, Priority.LOW, "HQ", "APP",
                LocalDateTime.now().plusHours(24), LocalDateTime.now()
        );
        ticket.setId(200L);

        when(ticketRepository.findById(200L)).thenReturn(Optional.of(ticket));

        UpdateTicketStatusUseCase.UpdateStatusCommand cmd = new UpdateTicketStatusUseCase.UpdateStatusCommand(
                200L,
                TicketStatus.CANCELLED,
                "Cancelling someone else ticket",
                "intruder@helpflow.com",
                Role.ROLE_USUARIO
        );

        assertThrows(UnauthorizedTicketAccessException.class, () -> ticketService.updateStatus(cmd));
    }

    @Test
    @DisplayName("Admin can assign technician to ticket")
    void testAdminAssignTechnician() {
        Ticket ticket = Ticket.createNew(
                activeUser.getId(), activeUser.getCorporateEmail(), "Network down", "No wifi",
                Category.NETWORK, Priority.HIGH, "HQ", "ROUTER",
                LocalDateTime.now().plusHours(4), LocalDateTime.now()
        );
        ticket.setId(300L);

        when(ticketRepository.findById(300L)).thenReturn(Optional.of(ticket));
        when(technicianRepository.findByCode("TEC-001")).thenReturn(Optional.of(activeTechnician));
        when(ticketRepository.countActiveTicketsByTechnicianId(10L)).thenReturn(1);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        AssignTechnicianUseCase.AssignTechnicianCommand cmd = new AssignTechnicianUseCase.AssignTechnicianCommand(
                300L, "TEC-001", "admin@helpflow.com", "Manual assignment"
        );

        Ticket updated = ticketService.assignTechnician(cmd);

        assertEquals(TicketStatus.ASSIGNED, updated.getStatus());
        assertEquals("TEC-001", updated.getAssignedTechnicianCode());
    }

    @Test
    @DisplayName("Admin can escalate ticket priority")
    void testEscalateTicket() {
        Ticket ticket = Ticket.createNew(
                activeUser.getId(), activeUser.getCorporateEmail(), "Slow PC", "Sluggish performance",
                Category.HARDWARE, Priority.LOW, "HQ", "PC-01",
                LocalDateTime.now().plusHours(24), LocalDateTime.now()
        );
        ticket.setId(400L);

        when(ticketRepository.findById(400L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        EscalateTicketUseCase.EscalateTicketCommand cmd = new EscalateTicketUseCase.EscalateTicketCommand(
                400L, Priority.CRITICAL, "Critical VIP user request", "admin@helpflow.com"
        );

        Ticket escalated = ticketService.escalateTicket(cmd);

        assertEquals(Priority.CRITICAL, escalated.getPriority());
    }

    @Test
    @DisplayName("checkAndEscalateApproachingBreachTickets escalates open tickets near deadline")
    void testCheckAndEscalateApproachingBreach() {
        LocalDateTime now = LocalDateTime.now();
        Ticket approachingTicket = Ticket.createNew(
                1L, "user@helpflow.com", "Approaching deadline", "Desc",
                Category.SOFTWARE, Priority.LOW, "HQ", "APP",
                now.plusMinutes(15), now.minusHours(23)
        );
        approachingTicket.setId(500L);

        when(ticketRepository.findOpenAndAssignedTickets()).thenReturn(List.of(approachingTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        int escalated = ticketService.checkAndEscalateApproachingBreachTickets(30);

        assertEquals(1, escalated);
        assertEquals(Priority.MEDIUM, approachingTicket.getPriority());
    }

    @Test
    @DisplayName("Create ticket falls back to specialty pool when dispatch suggested tech is overloaded")
    void testCreateTicketFallbackPool() {
        when(userRepository.findByCorporateEmail(activeUser.getCorporateEmail())).thenReturn(Optional.of(activeUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(101L);
            }
            return t;
        });

        // Dispatch returns TEC-001 who is already at maximum capacity (5/5)
        DispatchResult dispatchResult = new DispatchResult(101L, "TEC-001", Availability.HIGH, 2);
        when(dispatchPort.dispatch(eq(101L), eq(Category.HARDWARE), eq(Priority.HIGH))).thenReturn(dispatchResult);
        when(technicianRepository.findByCode("TEC-001")).thenReturn(Optional.of(activeTechnician));
        when(ticketRepository.countActiveTicketsByTechnicianId(10L)).thenReturn(5); // overloaded!

        // Fallback technician TEC-002
        Technician fallbackTech = new Technician(11L, "TEC-002", "DOC-T2", "Tech Two", "tech2@helpflow.com",
                "HQ", Set.of(Category.HARDWARE), TechnicianStatus.ACTIVE, 5);
        when(technicianRepository.findActiveBySpecialty(Category.HARDWARE)).thenReturn(List.of(fallbackTech));
        when(ticketRepository.countActiveTicketsByTechnicianId(11L)).thenReturn(1);

        CreateTicketUseCase.CreateTicketCommand cmd = new CreateTicketUseCase.CreateTicketCommand(
                activeUser.getCorporateEmail(), "Broken Mouse", "Right click broken",
                Category.HARDWARE, Priority.HIGH, "HQ", "MOU-01"
        );

        Ticket result = ticketService.createTicket(cmd);

        assertEquals(TicketStatus.ASSIGNED, result.getStatus());
        assertEquals("TEC-002", result.getAssignedTechnicianCode());
    }
}
