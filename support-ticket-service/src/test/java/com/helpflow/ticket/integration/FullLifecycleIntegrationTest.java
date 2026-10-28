package com.helpflow.ticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.ticket.application.port.out.DispatchResult;
import com.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.ticket.domain.model.Availability;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Full End-to-End Ticket Lifecycle Integration Test")
class FullLifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_e2e_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TechnicianDispatchPort dispatchPort;

    @Test
    @DisplayName("Complete E2E: Register -> Login -> Create Ticket -> Auto Assign -> In Progress -> Resolve -> Close")
    void testEndToEndLifecycle() throws Exception {
        // register
        String registerJson = """
            {
              "document": "DOC-E2E-001",
              "name": "E2E User",
              "corporateEmail": "e2e.user@helpflow.com",
              "password": "Password123!",
              "location": "Sede Principal",
              "area": "Ingenieria"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        // authenticate
        String loginJson = """
            {
              "email": "e2e.user@helpflow.com",
              "password": "Password123!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String userToken = loginNode.get("token").asText();

        // Admin login for administrative actions
        String adminLoginJson = """
            {
              "email": "admin@helpflow.com",
              "password": "Password123!"
            }
            """;

        MvcResult adminLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginJson))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        when(dispatchPort.dispatch(any(), eq(Category.HARDWARE), eq(Priority.HIGH)))
                .thenReturn(new DispatchResult(1L, "TEC-001", Availability.HIGH, 2));

        String createTicketJson = """
            {
              "title": "Broken Laptop Screen",
              "description": "Screen cracked during transport, needs replacement display",
              "category": "HARDWARE",
              "priority": "HIGH",
              "location": "Sede Principal",
              "affectedAsset": "LAPTOP-DEL-45"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTicketJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTechnicianCode").value("TEC-001"))
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.histories").isArray())
                .andReturn();

        Long ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // start progress
        String toInProgressJson = """
            {
              "status": "IN_PROGRESS",
              "observations": "Diagnosis started, replacement LCD screen requested from inventory"
            }
            """;

        mockMvc.perform(patch("/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toInProgressJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.firstResponseAt").isNotEmpty());

        // resolve
        String toResolvedJson = """
            {
              "status": "RESOLVED",
              "observations": "Display successfully replaced and tested. Laptop is operational."
            }
            """;

        mockMvc.perform(patch("/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toResolvedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // close
        String toClosedJson = """
            {
              "status": "CLOSED",
              "observations": "Received laptop, verified everything is working perfectly. Closing ticket."
            }
            """;

        mockMvc.perform(patch("/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toClosedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // invalid transition attempt
        String invalidTransitionJson = """
            {
              "status": "IN_PROGRESS",
              "observations": "Attempting invalid transition from terminal CLOSED status"
            }
            """;

        mockMvc.perform(patch("/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTransitionJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid State Transition"))
                .andExpect(jsonPath("$.status").value(409));

        // verify
        MvcResult finalResult = mockMvc.perform(get("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode finalTicket = objectMapper.readTree(finalResult.getResponse().getContentAsString());
        assertEquals("CLOSED", finalTicket.get("status").asText());
        assertEquals(5, finalTicket.get("histories").size()); // OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
    }
}
