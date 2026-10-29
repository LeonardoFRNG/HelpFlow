package com.helpflow.ticket.application.service;

import com.helpflow.ticket.application.port.in.AuthenticationUseCase;
import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.RefreshTokenRepositoryPort;
import com.helpflow.ticket.application.port.out.TokenProviderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.UserInactiveException;
import com.helpflow.ticket.domain.exception.UserNotFoundException;
import com.helpflow.ticket.domain.model.RefreshToken;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AuthApplicationService implements AuthenticationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserRepositoryPort userRepository;
    private final TokenProviderPort tokenProvider;
    private final PasswordEncoderPort passwordEncoder;
    private final UserManagementUseCase userManagementUseCase;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public AuthApplicationService(UserRepositoryPort userRepository,
                                  TokenProviderPort tokenProvider,
                                  PasswordEncoderPort passwordEncoder,
                                  UserManagementUseCase userManagementUseCase,
                                  RefreshTokenRepositoryPort refreshTokenRepository) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userManagementUseCase = userManagementUseCase;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByCorporateEmail(command.email().trim().toLowerCase())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessRuleException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UserInactiveException(user.getId());
        }

        String accessToken = tokenProvider.generateToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiryDate = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.create(user.getId(), tokenHash, expiryDate);
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, rawRefreshToken, user.getCorporateEmail(), user.getName(), user.getRole(), user.getId());
    }

    @Override
    @Transactional
    public AuthResult register(UserManagementUseCase.RegisterUserCommand command) {
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
        String accessToken = tokenProvider.generateToken(created);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiryDate = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.create(created.getId(), tokenHash, expiryDate);
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, rawRefreshToken, created.getCorporateEmail(), created.getName(), created.getRole(), created.getId());
    }

    @Override
    @Transactional
    public AuthResult refreshToken(RefreshTokenCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new BusinessRuleException("Refresh token is required");
        }

        String presentedTokenHash = tokenProvider.hashToken(command.refreshToken().trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(presentedTokenHash)
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        Instant now = Instant.now();

        // Deteccion de reutilizacion / ataque de repeticion
        if (storedToken.isRevoked()) {
            log.warn("Refresh token reuse detected for userId {}. Revoking all active tokens.", storedToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId(), now);
            throw new BusinessRuleException("Invalid refresh token: token has already been consumed");
        }

        if (storedToken.isExpired(now)) {
            throw new BusinessRuleException("Refresh token is expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(storedToken.getUserId()));

        if (!user.isActive()) {
            throw new UserInactiveException(user.getId());
        }

        // Rotacion: generar nuevo refresh token e invalidar el actual
        String newRawRefreshToken = tokenProvider.generateRefreshToken();
        String newTokenHash = tokenProvider.hashToken(newRawRefreshToken);

        storedToken.replaceBy(newTokenHash, now);
        refreshTokenRepository.save(storedToken);

        Instant newExpiry = now.plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        RefreshToken newRefreshToken = RefreshToken.create(user.getId(), newTokenHash, newExpiry);
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = tokenProvider.generateToken(user);
        return new AuthResult(newAccessToken, newRawRefreshToken, user.getCorporateEmail(), user.getName(), user.getRole(), user.getId());
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            return;
        }

        String tokenHash = tokenProvider.hashToken(command.refreshToken().trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }
}
