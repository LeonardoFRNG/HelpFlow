package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.UpdateProfileRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile and status management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserManagementUseCase userManagementUseCase;

    public UserController(UserManagementUseCase userManagementUseCase) {
        this.userManagementUseCase = userManagementUseCase;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns details of the currently authenticated user")
    public ResponseEntity<UserResponseDto> getCurrentUser(Principal principal) {
        User user = userManagementUseCase.getUserByEmail(principal.getName());
        return ResponseEntity.ok(UserResponseDto.fromDomain(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates editable profile fields (name, location, area)")
    public ResponseEntity<UserResponseDto> updateProfile(@RequestBody UpdateProfileRequestDto request, Principal principal) {
        User user = userManagementUseCase.getUserByEmail(principal.getName());
        UserManagementUseCase.UpdateProfileCommand command = new UserManagementUseCase.UpdateProfileCommand(
                request.name(),
                request.location(),
                request.area()
        );
        User updated = userManagementUseCase.updateUserProfile(user.getId(), command);
        return ResponseEntity.ok(UserResponseDto.fromDomain(updated));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all users", description = "Admin-only view of all registered users in the organization")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<User> users = userManagementUseCase.getAllUsers();
        return ResponseEntity.ok(users.stream().map(UserResponseDto::fromDomain).toList());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Toggle user active status", description = "Admin-only activation or deactivation of users")
    public ResponseEntity<UserResponseDto> toggleStatus(@PathVariable Long id, @RequestParam boolean active) {
        User updated = userManagementUseCase.toggleUserStatus(id, active);
        return ResponseEntity.ok(UserResponseDto.fromDomain(updated));
    }
}
