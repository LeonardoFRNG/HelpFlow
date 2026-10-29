package com.helpflow.ticket.application;

import com.helpflow.ticket.application.port.in.AuthenticationUseCase;
import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.RefreshTokenRepositoryPort;
import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.application.service.AuthApplicationService;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.model.RefreshToken;
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

import java.time.Instant;
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

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @InjectMocks
    private AuthApplicationService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "DOC-100", "Carlos Ruiz", "carlos.ruiz@helpflow.com", "encodedSecret",
                "HQ", "IT", Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    @DisplayName("Login with valid credentials returns AuthResult with token and refreshToken")
    void testLoginSuccess() {
        when(userRepository.findByCorporateEmail("carlos.ruiz@helpflow.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encodedSecret")).thenReturn(true);
        when(tokenProvider.generateToken(testUser)).thenReturn("valid.jwt.token");
        when(tokenProvider.generateRefreshToken()).thenReturn("raw.refresh.token");
        when(tokenProvider.hashToken("raw.refresh.token")).thenReturn("hashed.refresh.token");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthenticationUseCase.LoginCommand command = new AuthenticationUseCase.LoginCommand("carlos.ruiz@helpflow.com", "Password123!");
        AuthenticationUseCase.AuthResult result = authService.login(command);

        assertNotNull(result);
        assertEquals("valid.jwt.token", result.token());
        assertEquals("raw.refresh.token", result.refreshToken());
        assertEquals("carlos.ruiz@helpflow.com", result.email());
        assertEquals(Role.ROLE_USUARIO, result.role());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
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
    @DisplayName("Register user issues token and refresh token enforcing least privilege")
    void testRegisterUser() {
        UserManagementUseCase.RegisterUserCommand registerCmd = new UserManagementUseCase.RegisterUserCommand(
                "DOC-200", "Diana Prince", "diana.prince@helpflow.com", "SecretPass!", "HQ", "Finance", Role.ROLE_ADMIN
        );

        when(userManagementUseCase.registerUser(any())).thenReturn(testUser);
        when(tokenProvider.generateToken(testUser)).thenReturn("new.user.token");
        when(tokenProvider.generateRefreshToken()).thenReturn("new.refresh.token");
        when(tokenProvider.hashToken("new.refresh.token")).thenReturn("new.hashed.refresh.token");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthenticationUseCase.AuthResult result = authService.register(registerCmd);

        assertNotNull(result);
        assertEquals("new.user.token", result.token());
        assertEquals("new.refresh.token", result.refreshToken());
        verify(userManagementUseCase).registerUser(argThat(cmd -> cmd.role() == Role.ROLE_USUARIO));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh token successfully rotates token and returns new pair")
    void testRefreshTokenSuccess() {
        RefreshToken storedToken = RefreshToken.create(1L, "existing.hash", Instant.now().plusSeconds(3600));

        when(tokenProvider.hashToken("raw.valid.refresh")).thenReturn("existing.hash");
        when(refreshTokenRepository.findByTokenHash("existing.hash")).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateRefreshToken()).thenReturn("rotated.refresh.token");
        when(tokenProvider.hashToken("rotated.refresh.token")).thenReturn("rotated.hash");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(tokenProvider.generateToken(testUser)).thenReturn("rotated.access.token");

        AuthenticationUseCase.AuthResult result = authService.refreshToken(new AuthenticationUseCase.RefreshTokenCommand("raw.valid.refresh"));

        assertNotNull(result);
        assertEquals("rotated.access.token", result.token());
        assertEquals("rotated.refresh.token", result.refreshToken());
        assertTrue(storedToken.isRevoked());
        assertEquals("rotated.hash", storedToken.getReplacedByTokenHash());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Reuse of revoked refresh token revokes all user tokens and throws BusinessRuleException")
    void testRefreshTokenReuseAttackDetected() {
        RefreshToken revokedToken = RefreshToken.create(1L, "compromised.hash", Instant.now().plusSeconds(3600));
        revokedToken.revoke(Instant.now());

        when(tokenProvider.hashToken("reused.token")).thenReturn("compromised.hash");
        when(refreshTokenRepository.findByTokenHash("compromised.hash")).thenReturn(Optional.of(revokedToken));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.refreshToken(new AuthenticationUseCase.RefreshTokenCommand("reused.token")));

        assertTrue(ex.getMessage().contains("reuse detected") || ex.getMessage().contains("already been consumed"));
        verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(Instant.class));
    }

    @Test
    @DisplayName("Expired refresh token throws BusinessRuleException")
    void testRefreshTokenExpired() {
        RefreshToken expiredToken = RefreshToken.create(1L, "expired.hash", Instant.now().minusSeconds(3600));

        when(tokenProvider.hashToken("expired.token")).thenReturn("expired.hash");
        when(refreshTokenRepository.findByTokenHash("expired.hash")).thenReturn(Optional.of(expiredToken));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.refreshToken(new AuthenticationUseCase.RefreshTokenCommand("expired.token")));

        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Logout revokes the refresh token")
    void testLogoutRevokesToken() {
        RefreshToken activeToken = RefreshToken.create(1L, "logout.hash", Instant.now().plusSeconds(3600));

        when(tokenProvider.hashToken("token.to.logout")).thenReturn("logout.hash");
        when(refreshTokenRepository.findByTokenHash("logout.hash")).thenReturn(Optional.of(activeToken));

        authService.logout(new AuthenticationUseCase.LogoutCommand("token.to.logout"));

        assertTrue(activeToken.isRevoked());
        verify(refreshTokenRepository).save(activeToken);
    }
}
