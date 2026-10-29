package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.model.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RefreshToken Domain Model Unit Tests")
class RefreshTokenTest {

    @Test
    @DisplayName("RefreshToken creation and state transitions")
    void testRefreshTokenLifecycle() {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        RefreshToken token = RefreshToken.create(1L, "sample_hash", expiry);

        assertNull(token.getId());
        assertEquals(1L, token.getUserId());
        assertEquals("sample_hash", token.getTokenHash());
        assertEquals(expiry, token.getExpiryDate());
        assertFalse(token.isRevoked());
        assertNull(token.getRevokedAt());
        assertNull(token.getReplacedByTokenHash());
        assertNotNull(token.getCreatedAt());
        assertTrue(token.isActive(now));
        assertFalse(token.isExpired(now));

        // Test setters
        token.setId(10L);
        assertEquals(10L, token.getId());
        token.setUserId(2L);
        assertEquals(2L, token.getUserId());
        token.setTokenHash("new_hash");
        assertEquals("new_hash", token.getTokenHash());
        Instant newExpiry = expiry.plusSeconds(1800);
        token.setExpiryDate(newExpiry);
        assertEquals(newExpiry, token.getExpiryDate());
        Instant newCreated = now.minusSeconds(100);
        token.setCreatedAt(newCreated);
        assertEquals(newCreated, token.getCreatedAt());

        // Test replacement
        Instant replaceTime = now.plusSeconds(10);
        token.replaceBy("replacement_hash", replaceTime);

        assertTrue(token.isRevoked());
        assertEquals(replaceTime, token.getRevokedAt());
        assertEquals("replacement_hash", token.getReplacedByTokenHash());
        assertFalse(token.isActive(now));

        // Test expiration check with past date
        Instant pastExpiry = now.minusSeconds(10);
        RefreshToken expiredToken = RefreshToken.create(3L, "hash_exp", pastExpiry);
        assertTrue(expiredToken.isExpired(now));
        assertFalse(expiredToken.isActive(now));

        // Test no-args constructor
        RefreshToken emptyToken = new RefreshToken();
        emptyToken.setRevoked(true);
        assertTrue(emptyToken.isRevoked());
        emptyToken.setRevokedAt(replaceTime);
        assertEquals(replaceTime, emptyToken.getRevokedAt());
        emptyToken.setReplacedByTokenHash("rep_hash");
        assertEquals("rep_hash", emptyToken.getReplacedByTokenHash());
    }
}
