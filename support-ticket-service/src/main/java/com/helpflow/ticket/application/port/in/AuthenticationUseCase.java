package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Role;

public interface AuthenticationUseCase {

    record LoginCommand(
            String email,
            String password
    ) {}

    record RefreshTokenCommand(
            String refreshToken
    ) {}

    record LogoutCommand(
            String refreshToken
    ) {}

    record AuthResult(
            String token,
            String refreshToken,
            String email,
            String name,
            Role role,
            Long userId
    ) {
        public AuthResult(String token, String email, String name, Role role, Long userId) {
            this(token, null, email, name, role, userId);
        }
    }

    AuthResult login(LoginCommand command);

    AuthResult register(UserManagementUseCase.RegisterUserCommand command);

    AuthResult refreshToken(RefreshTokenCommand command);

    void logout(LogoutCommand command);
}
