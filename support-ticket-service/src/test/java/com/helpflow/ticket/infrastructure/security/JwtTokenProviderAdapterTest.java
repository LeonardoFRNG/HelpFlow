package com.helpflow.ticket.infrastructure.security;

import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Token Provider Unit Tests")
class JwtTokenProviderAdapterTest {

    private JwtTokenProviderAdapter provider;
    private User testUser;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProviderAdapter(
                "super-secure-secret-key-at-least-256-bits-long-for-testing-purposes-1234567890",
                3600000 // 1 hour
        );

        testUser = new User(42L, "DOC-99", "Admin User", "admin@helpflow.com", "pass",
                "HQ", "IT", Role.ROLE_ADMIN, UserStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    @DisplayName("Generate token and successfully validate signature and claims")
    void testGenerateAndValidate() {
        String token = provider.generateToken(testUser);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("admin@helpflow.com", provider.extractUsername(token));
        assertEquals(Role.ROLE_ADMIN, provider.extractRole(token));
        assertEquals(42L, provider.extractUserId(token));
    }

    @Test
    @DisplayName("Invalid or tampered token fails validation")
    void testInvalidToken() {
        assertFalse(provider.validateToken("not.a.real.jwt"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken(null));

        String token = provider.generateToken(testUser);
        String tampered = token + "xyz";
        assertFalse(provider.validateToken(tampered));
    }

    @Test
    @DisplayName("Expired token fails validation")
    void testExpiredToken() {
        JwtTokenProviderAdapter expiredProvider = new JwtTokenProviderAdapter(
                "super-secure-secret-key-at-least-256-bits-long-for-testing-purposes-1234567890",
                -1000 // expired 1 sec ago
        );

        String expiredToken = expiredProvider.generateToken(testUser);
        assertFalse(expiredProvider.validateToken(expiredToken));
    }
}
