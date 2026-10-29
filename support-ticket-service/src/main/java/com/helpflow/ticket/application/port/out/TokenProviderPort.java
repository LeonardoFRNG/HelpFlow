package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;

public interface TokenProviderPort {

    String generateToken(User user);

    boolean validateToken(String token);

    String extractUsername(String token);

    Role extractRole(String token);

    String generateRefreshToken();

    String hashToken(String rawToken);

    long getRefreshTokenExpirationMs();
}
