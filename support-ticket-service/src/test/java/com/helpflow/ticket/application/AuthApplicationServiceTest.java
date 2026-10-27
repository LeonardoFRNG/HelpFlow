package com.helpflow.ticket.application;

import com.helpflow.ticket.application.port.in.AuthenticationUseCase;
import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.application.service.AuthApplicationService;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Application Service Unit Tests")
class AuthApplicationServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TokenProviderPort tokenProvider;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private UserManagementUseCase userManagementUseCase;

    @InjectMocks
    private AuthApplicationService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "DOC-100", "Carlos Ruiz", "carlos.ruiz@helpflow.com", "encodedSecret",
                "HQ", "IT", Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    @DisplayName("Login with valid credentials returns AuthResult with token")
    void testLoginSuccess() {
        when(userRepository.findByCorporateEmail("carlos.ruiz@helpflow.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encodedSecret")).thenReturn(true);
        when(tokenProvider.generateToken(testUser)).thenReturn("valid.jwt.token");

        AuthenticationUseCase.LoginCommand command = new AuthenticationUseCase.LoginCommand("carlos.ruiz@helpflow.com", "Password123!");
        AuthenticationUseCase.AuthResult result = authService.login(command);

        assertNotNull(result);
        assertEquals("valid.jwt.token", result.token());
        assertEquals("carlos.ruiz@helpflow.com", result.email());
        assertEquals(Role.ROLE_USUARIO, result.role());
    }

    @Test
    @DisplayName("Login with invalid password throws BusinessRuleException")
    void testLoginInvalidPassword() {
        when(userRepository.findByCorporateEmail("carlos.ruiz@helpflow.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encodedSecret")).thenReturn(false);

        AuthenticationUseCase.LoginCommand command = new AuthenticationUseCase.LoginCommand("carlos.ruiz@helpflow.com", "WrongPassword");

        assertThrows(BusinessRuleException.class, () -> authService.login(command));
    }

    @Test
    @DisplayName("Login with inactive user throws UserInactiveException")
    void testLoginInactiveUser() {
        testUser.deactivate();
        when(userRepository.findByCorporateEmail("carlos.ruiz@helpflow.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encodedSecret")).thenReturn(true);

        AuthenticationUseCase.LoginCommand command = new AuthenticationUseCase.LoginCommand("carlos.ruiz@helpflow.com", "Password123!");

        assertThrows(UserInactiveException.class, () -> authService.login(command));
    }

    @Test
    @DisplayName("Register user issues token and enforces least privilege")
    void testRegisterUser() {
        UserManagementUseCase.RegisterUserCommand registerCmd = new UserManagementUseCase.RegisterUserCommand(
                "DOC-200", "Diana Prince", "diana.prince@helpflow.com", "SecretPass!", "HQ", "Finance", Role.ROLE_ADMIN
        );

        when(userManagementUseCase.registerUser(any())).thenReturn(testUser);
        when(tokenProvider.generateToken(testUser)).thenReturn("new.user.token");

        AuthenticationUseCase.AuthResult result = authService.register(registerCmd);

        assertNotNull(result);
        assertEquals("new.user.token", result.token());
        // Verify register was invoked with safe command (ROLE_ADMIN downgraded to ROLE_USUARIO for public registration)
        verify(userManagementUseCase).registerUser(argThat(cmd -> cmd.role() == Role.ROLE_USUARIO));
    }
}
