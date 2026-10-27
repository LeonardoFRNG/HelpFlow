package com.helpflow.ticket.integration;

import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Authentication REST API Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_auth_test")
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
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /auth/register with valid corporate user creates user and returns JWT token")
    void testRegisterSuccess() throws Exception {
        String json = """
            {
              "document": "DOC-REG-101",
              "name": "Maria Lopez",
              "corporateEmail": "maria.lopez@helpflow.com",
              "password": "Password123!",
              "location": "Sede Principal",
              "area": "Recursos Humanos"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("maria.lopez@helpflow.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USUARIO"));
    }

    @Test
    @DisplayName("POST /auth/register with non-corporate email returns 400 ProblemDetail")
    void testRegisterInvalidEmail() throws Exception {
        String json = """
            {
              "document": "DOC-REG-102",
              "name": "Hacker",
              "corporateEmail": "hacker@randomgmail.com",
              "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials returns 200 and JWT token")
    void testLoginSuccess() throws Exception {
        userRepository.save(new User(
                null, "DOC-LOG-1", "Test Login User", "login.user@helpflow.com",
                passwordEncoder.encode("SecretPass123!"), "HQ", "IT",
                Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
        ));

        String json = """
            {
              "email": "login.user@helpflow.com",
              "password": "SecretPass123!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login.user@helpflow.com"));
    }

    @Test
    @DisplayName("POST /auth/login with invalid password returns 400 ProblemDetail")
    void testLoginWrongPassword() throws Exception {
        String json = """
            {
              "email": "login.user@helpflow.com",
              "password": "WrongPassword123"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Business Rule Violation"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
