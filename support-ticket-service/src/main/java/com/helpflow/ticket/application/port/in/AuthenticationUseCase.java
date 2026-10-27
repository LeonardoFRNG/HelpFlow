package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Role;

public interface AuthenticationUseCase {

    record LoginCommand(
            String email,
            String password
    ) {}

    record AuthResult(
            String token,
            String email,
            String name,
            Role role,
            Long userId
    ) {}

    AuthResult login(LoginCommand command);

    AuthResult register(UserManagementUseCase.RegisterUserCommand command);
}
