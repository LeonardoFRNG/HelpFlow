package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.RefreshTokenRepositoryPort;
import com.helpflow.ticket.domain.model.RefreshToken;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataRefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final SpringDataRefreshTokenRepository repository;

    public RefreshTokenPersistenceAdapter(SpringDataRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = toEntity(refreshToken);
        RefreshTokenJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllByUserId(Long userId, Instant now) {
        repository.revokeAllByUserId(userId, now);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens(Instant now) {
        repository.deleteExpiredTokens(now);
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken domain) {
        if (domain == null) return null;
        return new RefreshTokenJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getTokenHash(),
                domain.getExpiryDate(),
                domain.isRevoked(),
                domain.getRevokedAt(),
                domain.getReplacedByTokenHash(),
                domain.getCreatedAt()
        );
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        if (entity == null) return null;
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiryDate(),
                entity.isRevoked(),
                entity.getRevokedAt(),
                entity.getReplacedByTokenHash(),
                entity.getCreatedAt()
        );
    }
}
