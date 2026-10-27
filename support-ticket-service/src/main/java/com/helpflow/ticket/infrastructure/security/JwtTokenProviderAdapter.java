package com.helpflow.ticket.infrastructure.security;

import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderAdapter.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProviderAdapter(
            @Value("${jwt.secret:helpflow-super-secret-jwt-signing-key-for-production-grade-security-at-least-256-bits!}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getCorporateEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token.trim());
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token is expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    @Override
    public Role extractRole(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null && claims.get("role") != null) {
            return Role.fromString(claims.get("role", String.class));
        }
        return Role.ROLE_USUARIO;
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null && claims.get("userId") != null) {
            return claims.get("userId", Long.class);
        }
        return null;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Failed to parse claims from JWT token: {}", ex.getMessage());
            return null;
        }
    }
}
