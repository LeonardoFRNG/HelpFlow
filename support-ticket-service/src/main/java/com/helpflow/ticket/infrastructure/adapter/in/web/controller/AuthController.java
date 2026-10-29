package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.AuthenticationUseCase;
import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.AuthResponseDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.LoginRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.LogoutRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.RefreshTokenRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.RegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and JWT authentication")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;

    public AuthController(AuthenticationUseCase authenticationUseCase) {
        this.authenticationUseCase = authenticationUseCase;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Registers an active user with corporate email and returns access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        UserManagementUseCase.RegisterUserCommand command = new UserManagementUseCase.RegisterUserCommand(
                request.document(),
                request.name(),
                request.corporateEmail(),
                request.password(),
                request.location(),
                request.area(),
                request.role() != null ? request.role() : Role.ROLE_USUARIO
        );

        AuthenticationUseCase.AuthResult result = authenticationUseCase.register(command);
        AuthResponseDto response = AuthResponseDto.of(
                result.token(),
                result.refreshToken(),
                result.email(),
                result.name(),
                result.role(),
                result.userId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login to the system", description = "Authenticates user credentials and issues access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthenticationUseCase.LoginCommand command = new AuthenticationUseCase.LoginCommand(
                request.email(),
                request.password()
        );

        AuthenticationUseCase.AuthResult result = authenticationUseCase.login(command);
        AuthResponseDto response = AuthResponseDto.of(
                result.token(),
                result.refreshToken(),
                result.email(),
                result.name(),
                result.role(),
                result.userId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Validates the refresh token, performs rotation, and returns a new access/refresh token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid, expired, or consumed refresh token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthenticationUseCase.RefreshTokenCommand command = new AuthenticationUseCase.RefreshTokenCommand(request.refreshToken());
        AuthenticationUseCase.AuthResult result = authenticationUseCase.refreshToken(command);
        AuthResponseDto response = AuthResponseDto.of(
                result.token(),
                result.refreshToken(),
                result.email(),
                result.name(),
                result.role(),
                result.userId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the supplied refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequestDto request) {
        authenticationUseCase.logout(new AuthenticationUseCase.LogoutCommand(request.refreshToken()));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
