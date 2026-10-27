package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import com.helpflow.ticket.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Unit Tests")
class UserControllerWebTest {

    private MockMvc mockMvc;

    @Mock
    private UserManagementUseCase userManagementUseCase;

    @InjectMocks
    private UserController userController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleUser = new User(1L, "DOC-123", "Alice Smith", "alice.smith@helpflow.com", "pass",
                "HQ", "Finance", Role.ROLE_USUARIO, UserStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /users/me returns authenticated user profile")
    void testGetMe() throws Exception {
        when(userManagementUseCase.getUserByEmail("alice.smith@helpflow.com")).thenReturn(sampleUser);

        Principal principal = () -> "alice.smith@helpflow.com";

        mockMvc.perform(get("/users/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corporateEmail").value("alice.smith@helpflow.com"))
                .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    @DisplayName("PUT /users/me updates profile")
    void testUpdateProfile() throws Exception {
        when(userManagementUseCase.getUserByEmail("alice.smith@helpflow.com")).thenReturn(sampleUser);
        when(userManagementUseCase.updateUserProfile(eq(1L), any())).thenReturn(sampleUser);

        Principal principal = () -> "alice.smith@helpflow.com";

        String json = """
            {
              "name": "Alice Johnson",
              "location": "North Branch",
              "area": "Accounting"
            }
            """;

        mockMvc.perform(put("/users/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users returns all users")
    void testGetAllUsers() throws Exception {
        when(userManagementUseCase.getAllUsers()).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PATCH /users/{id}/status toggles active status")
    void testToggleStatus() throws Exception {
        when(userManagementUseCase.toggleUserStatus(1L, false)).thenReturn(sampleUser);

        mockMvc.perform(patch("/users/1/status?active=false"))
                .andExpect(status().isOk());
    }
}
