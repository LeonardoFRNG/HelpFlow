package com.helpflow.ticket.application.service;

import com.helpflow.ticket.application.port.in.AuthenticationUseCase;
import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthApplicationService implements AuthenticationUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenProviderPort tokenProvider;
    private final PasswordEncoderPort passwordEncoder;
    private final UserManagementUseCase userManagementUseCase;

    public AuthApplicationService(UserRepositoryPort userRepository,
                                  TokenProviderPort tokenProvider,
                                  PasswordEncoderPort passwordEncoder,
                                  UserManagementUseCase userManagementUseCase) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userManagementUseCase = userManagementUseCase;
    }

    @Override
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByCorporateEmail(command.email().trim().toLowerCase())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessRuleException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UserInactiveException(user.getId());
        }

        String token = tokenProvider.generateToken(user);
        return new AuthResult(token, user.getCorporateEmail(), user.getName(), user.getRole(), user.getId());
    }

    @Override
    @Transactional
    public AuthResult register(UserManagementUseCase.RegisterUserCommand command) {
        // Enforce least privilege on public registration: default to ROLE_USUARIO
        Role role = command.role() == Role.ROLE_ADMIN ? Role.ROLE_USUARIO : (command.role() != null ? command.role() : Role.ROLE_USUARIO);

        UserManagementUseCase.RegisterUserCommand safeCommand = new UserManagementUseCase.RegisterUserCommand(
                command.document(),
                command.name(),
                command.corporateEmail(),
                command.password(),
                command.location(),
                command.area(),
                role
        );

        User created = userManagementUseCase.registerUser(safeCommand);
        String token = tokenProvider.generateToken(created);

        return new AuthResult(token, created.getCorporateEmail(), created.getName(), created.getRole(), created.getId());
    }
}
