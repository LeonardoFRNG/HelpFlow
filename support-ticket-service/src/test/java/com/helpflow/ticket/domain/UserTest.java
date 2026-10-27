package com.helpflow.ticket.domain;

import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Entity Tests")
class UserTest {

    @Test
    @DisplayName("Valid user creation succeeds")
    void testCreateValidUser() {
        User user = User.createNew(
                "DOC-12345",
                "Carlos Perez",
                "carlos.perez@helpflow.com",
                "hashedPass",
                "Sede Principal",
                "TI",
                Role.ROLE_USUARIO
        );

        assertNotNull(user);
        assertEquals("DOC-12345", user.getDocument());
        assertEquals("carlos.perez@helpflow.com", user.getCorporateEmail());
        assertTrue(user.isActive());
        assertEquals(Role.ROLE_USUARIO, user.getRole());
    }

    @Test
    @DisplayName("Invalid corporate email throws BusinessRuleException")
    void testInvalidEmail() {
        assertThrows(BusinessRuleException.class, () ->
                User.createNew("DOC-999", "Invalid Email User", "notanemail", "pass", "HQ", "IT", Role.ROLE_USUARIO)
        );
        assertThrows(BusinessRuleException.class, () ->
                User.createNew("DOC-999", "Empty Email User", "", "pass", "HQ", "IT", Role.ROLE_USUARIO)
        );
    }

    @Test
    @DisplayName("User status toggle works")
    void testStatusToggle() {
        User user = User.createNew("DOC-111", "Ana Gomez", "ana.gomez@helpflow.com", "pass", "HQ", "HR", Role.ROLE_USUARIO);
        assertTrue(user.isActive());

        user.deactivate();
        assertFalse(user.isActive());
        assertEquals(UserStatus.INACTIVE, user.getStatus());

        user.activate();
        assertTrue(user.isActive());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }
}
