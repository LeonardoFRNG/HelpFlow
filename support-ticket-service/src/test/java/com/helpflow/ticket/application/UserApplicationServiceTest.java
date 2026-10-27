package com.helpflow.ticket.application;

import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.application.service.UserApplicationService;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Application Service Unit Tests")
class UserApplicationServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UserApplicationService userService;

    @Test
    @DisplayName("Duplicate corporate email throws BusinessRuleException")
    void testDuplicateEmail() {
        when(userRepository.existsByCorporateEmail("existing@helpflow.com")).thenReturn(true);

        UserManagementUseCase.RegisterUserCommand cmd = new UserManagementUseCase.RegisterUserCommand(
                "DOC-99", "Name", "existing@helpflow.com", "pass", "HQ", "IT", Role.ROLE_USUARIO
        );

        assertThrows(BusinessRuleException.class, () -> userService.registerUser(cmd));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate document throws BusinessRuleException")
    void testDuplicateDocument() {
        when(userRepository.existsByCorporateEmail("new@helpflow.com")).thenReturn(false);
        when(userRepository.existsByDocument("DOC-EXISTING")).thenReturn(true);

        UserManagementUseCase.RegisterUserCommand cmd = new UserManagementUseCase.RegisterUserCommand(
                "DOC-EXISTING", "Name", "new@helpflow.com", "pass", "HQ", "IT", Role.ROLE_USUARIO
        );

        assertThrows(BusinessRuleException.class, () -> userService.registerUser(cmd));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Toggle user status updates user status")
    void testToggleUserStatus() {
        User user = User.createNew("DOC-1", "Name", "user@helpflow.com", "pass", "HQ", "IT", Role.ROLE_USUARIO);
        user.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User deactivated = userService.toggleUserStatus(10L, false);
        assertEquals(UserStatus.INACTIVE, deactivated.getStatus());

        User activated = userService.toggleUserStatus(10L, true);
        assertEquals(UserStatus.ACTIVE, activated.getStatus());
    }
}
