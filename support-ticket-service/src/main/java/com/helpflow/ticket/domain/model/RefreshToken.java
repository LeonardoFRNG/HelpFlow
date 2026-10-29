package com.helpflow.ticket.domain.model;

import java.time.Instant;
import java.util.Objects;

public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiryDate;
    private boolean revoked;
    private Instant revokedAt;
    private String replacedByTokenHash;
    private Instant createdAt;

    public RefreshToken() {
    }

    public RefreshToken(Long id, Long userId, String tokenHash, Instant expiryDate,
                        boolean revoked, Instant revokedAt, String replacedByTokenHash, Instant createdAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate is required");
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedByTokenHash = replacedByTokenHash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static RefreshToken create(Long userId, String tokenHash, Instant expiryDate) {
        return new RefreshToken(null, userId, tokenHash, expiryDate, false, null, null, Instant.now());
    }

    public boolean isExpired(Instant now) {
        Instant checkTime = now != null ? now : Instant.now();
        return checkTime.isAfter(this.expiryDate);
    }

    public boolean isActive(Instant now) {
        return !this.revoked && !isExpired(now);
    }

    public void revoke(Instant now) {
        this.revoked = true;
        this.revokedAt = now != null ? now : Instant.now();
    }

    public void replaceBy(String newTokenHash, Instant now) {
        revoke(now);
        this.replacedByTokenHash = newTokenHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
    }

    public void setReplacedByTokenHash(String replacedByTokenHash) {
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
