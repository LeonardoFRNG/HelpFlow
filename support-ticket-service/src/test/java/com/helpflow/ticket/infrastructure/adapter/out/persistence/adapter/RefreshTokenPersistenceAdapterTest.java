package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.domain.model.RefreshToken;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataRefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenPersistenceAdapter Unit Tests")
class RefreshTokenPersistenceAdapterTest {

    @Mock
    private SpringDataRefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenPersistenceAdapter adapter;

    @Test
    @DisplayName("save and findByTokenHash map properly to and from domain")
    void testSaveAndFind() {
        Instant now = Instant.now();
        RefreshToken domain = new RefreshToken(1L, 2L, "hash123", now.plusSeconds(3600), false, null, null, now);
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(1L, 2L, "hash123", now.plusSeconds(3600), false, null, null, now);

        when(repository.save(any(RefreshTokenJpaEntity.class))).thenReturn(entity);
        when(repository.findByTokenHash("hash123")).thenReturn(Optional.of(entity));

        RefreshToken saved = adapter.save(domain);
        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals("hash123", saved.getTokenHash());

        Optional<RefreshToken> found = adapter.findByTokenHash("hash123");
        assertTrue(found.isPresent());
        assertEquals(2L, found.get().getUserId());

        // Test null handling
        assertNull(adapter.save(null));

        // Test deleteExpiredTokens and revokeAll
        adapter.revokeAllByUserId(2L, now);
        verify(repository).revokeAllByUserId(2L, now);

        adapter.deleteExpiredTokens(now);
        verify(repository).deleteExpiredTokens(now);
    }
}
