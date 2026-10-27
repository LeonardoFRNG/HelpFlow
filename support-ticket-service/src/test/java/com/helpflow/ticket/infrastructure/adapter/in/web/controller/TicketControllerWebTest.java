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
import com.helpflow.ticket.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ticket Controller Unit Tests")
class TicketControllerWebTest {

    private MockMvc mockMvc;

    @Mock
    private CreateTicketUseCase createTicketUseCase;

    @Mock
    private GetTicketUseCase getTicketUseCase;

    @Mock
    private UpdateTicketStatusUseCase updateTicketStatusUseCase;

    @Mock
    private AssignTechnicianUseCase assignTechnicianUseCase;

    @Mock
    private EscalateTicketUseCase escalateTicketUseCase;

    @InjectMocks
    private TicketController ticketController;

    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleTicket = Ticket.createNew(
                1L, "user@helpflow.com", "Screen glitch", "Display flickers",
                Category.HARDWARE, Priority.MEDIUM, "HQ", "SCREEN-1",
                LocalDateTime.now().plusHours(8), LocalDateTime.now()
        );
        sampleTicket.setId(10L);
    }

    @Test
    @DisplayName("GET /tickets with filters returns filtered list")
    void testGetTicketsWithFilters() throws Exception {
        when(getTicketUseCase.getTickets(eq("admin@helpflow.com"), eq(Role.ROLE_ADMIN), any()))
                .thenReturn(List.of(sampleTicket));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@helpflow.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/tickets?status=OPEN&priority=MEDIUM&category=HARDWARE")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /tickets/{id} returns ticket details")
    void testGetTicketById() throws Exception {
        when(getTicketUseCase.getTicketById(eq(10L), eq("user@helpflow.com"), eq(Role.ROLE_USUARIO)))
                .thenReturn(sampleTicket);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user@helpflow.com", null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));

        mockMvc.perform(get("/tickets/10").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Screen glitch"));
    }

    @Test
    @DisplayName("POST /tickets/{id}/assign assigns technician")
    void testAssignTechnician() throws Exception {
        when(assignTechnicianUseCase.assignTechnician(any())).thenReturn(sampleTicket);

        Principal principal = () -> "admin@helpflow.com";

        String json = """
            {
              "technicianCode": "TEC-001",
              "notes": "Assigning to tech 1"
            }
            """;

        mockMvc.perform(post("/tickets/10/assign")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /tickets/{id}/escalate escalates ticket")
    void testEscalateTicket() throws Exception {
        when(escalateTicketUseCase.escalateTicket(any())).thenReturn(sampleTicket);

        Principal principal = () -> "admin@helpflow.com";

        String json = """
            {
              "targetPriority": "CRITICAL",
              "reason": "VIP customer escalation"
            }
            """;

        mockMvc.perform(post("/tickets/10/escalate")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
