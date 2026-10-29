package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(Long userId, Instant now);

    void deleteExpiredTokens(Instant now);
}
