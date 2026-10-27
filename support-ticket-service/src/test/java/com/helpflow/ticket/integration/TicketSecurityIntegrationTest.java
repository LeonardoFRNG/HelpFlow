package com.helpflow.ticket.integration;

import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Security & Role-Based Access Control Integration Tests")
class TicketSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_security_test")
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
    private UserRepositoryPort userRepository;

    @Autowired
    private TokenProviderPort tokenProvider;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User regularUser = userRepository.findByCorporateEmail("standard.user@helpflow.com")
                .orElseGet(() -> userRepository.save(new User(
                        null, "DOC-SEC-U1", "Standard User", "standard.user@helpflow.com", "pass",
                        "HQ", "Operations", Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
                )));
        userToken = tokenProvider.generateToken(regularUser);

        User adminUser = userRepository.findByCorporateEmail("admin@helpflow.com").orElseThrow();
        adminToken = tokenProvider.generateToken(adminUser);
    }

    @Test
    @DisplayName("Request without JWT token returns 401 Unauthorized with RFC 7807 ProblemDetail")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Regular user accessing admin endpoint returns 403 Forbidden with ProblemDetail")
    void testAdminEndpointRestricted() throws Exception {
        String assignPayload = """
            {
              "technicianCode": "TEC-001"
            }
            """;

        mockMvc.perform(post("/tickets/1/assign")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
