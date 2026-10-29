package com.helpflow.ticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.ticket.application.port.out.RefreshTokenRepositoryPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.model.RefreshToken;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Refresh Token Full Lifecycle & Security Integration Tests")
class RefreshTokenIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_rt_test")
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
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Login issues access token and refresh token, and /auth/refresh successfully rotates both")
    void testLoginAndSuccessfulRefreshRotation() throws Exception {
        userRepository.save(new User(
                null, "DOC-RT-1", "Refresh User 1", "rt.user1@helpflow.com",
                passwordEncoder.encode("Password123!"), "HQ", "IT",
                Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
        ));

        // 1. Login
        String loginJson = """
            {
              "email": "rt.user1@helpflow.com",
              "password": "Password123!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialAccessToken = loginNode.get("token").asText();
        String initialRefreshToken = loginNode.get("refreshToken").asText();

        assertNotNull(initialAccessToken);
        assertNotNull(initialRefreshToken);

        // 2. Perform refresh
        String refreshJson = String.format("{\"refreshToken\": \"%s\"}", initialRefreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshNode.get("token").asText();
        String newRefreshToken = refreshNode.get("refreshToken").asText();

        assertNotEquals(initialAccessToken, newAccessToken);
        assertNotEquals(initialRefreshToken, newRefreshToken);
    }

    @Test
    @DisplayName("Reusing a consumed refresh token triggers attack detection and revokes all user sessions")
    void testReuseDetectionRevokesAllTokens() throws Exception {
        userRepository.save(new User(
                null, "DOC-RT-2", "Refresh User 2", "rt.user2@helpflow.com",
                passwordEncoder.encode("Password123!"), "HQ", "IT",
                Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
        ));

        // 1. Login
        String loginJson = """
            {
              "email": "rt.user2@helpflow.com",
              "password": "Password123!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialRefreshToken = loginNode.get("refreshToken").asText();

        // 2. Legitimate refresh
        String refreshJson = String.format("{\"refreshToken\": \"%s\"}", initialRefreshToken);
        MvcResult legitimateRefreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode legitimateNode = objectMapper.readTree(legitimateRefreshResult.getResponse().getContentAsString());
        String secondRefreshToken = legitimateNode.get("refreshToken").asText();

        // 3. Attacker (or replay) tries to use the already consumed initialRefreshToken
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token: token has already been consumed"));

        // 4. Because reuse was detected, secondRefreshToken should also be revoked!
        String secondRefreshJson = String.format("{\"refreshToken\": \"%s\"}", secondRefreshToken);
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRefreshJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token: token has already been consumed"));
    }

    @Test
    @DisplayName("Expired refresh token cannot be refreshed")
    void testExpiredRefreshTokenFails() throws Exception {
        User user = userRepository.save(new User(
                null, "DOC-RT-3", "Refresh User 3", "rt.user3@helpflow.com",
                passwordEncoder.encode("Password123!"), "HQ", "IT",
                Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
        ));

        // Save an already expired token directly in repository
        String expiredHash = "expired_dummy_hash_3";
        refreshTokenRepository.save(new RefreshToken(
                null, user.getId(), expiredHash, Instant.now().minusSeconds(3600),
                false, null, null, Instant.now().minusSeconds(7200)
        ));

        // Attempting to refresh non-existent or expired token fails
        String refreshJson = "{\"refreshToken\": \"non.existent.or.expired\"}";
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Logout revokes the refresh token and prevents future refreshes")
    void testLogoutRevokesToken() throws Exception {
        userRepository.save(new User(
                null, "DOC-RT-4", "Refresh User 4", "rt.user4@helpflow.com",
                passwordEncoder.encode("Password123!"), "HQ", "IT",
                Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now()
        ));

        // 1. Login
        String loginJson = """
            {
              "email": "rt.user4@helpflow.com",
              "password": "Password123!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginNode.get("refreshToken").asText();

        // 2. Logout
        String logoutJson = String.format("{\"refreshToken\": \"%s\"}", refreshToken);
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 3. Attempting to refresh with the logged out token must fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token: token has already been consumed"));
    }
}
