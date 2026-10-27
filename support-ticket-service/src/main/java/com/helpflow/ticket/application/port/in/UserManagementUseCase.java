package com.helpflow.ticket.application.port.in;

import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;

import java.util.List;

public interface UserManagementUseCase {

    record RegisterUserCommand(
            String document,
            String name,
            String corporateEmail,
            String password,
            String location,
            String area,
            Role role
    ) {}

    record UpdateProfileCommand(
            String name,
            String location,
            String area
    ) {}

    User registerUser(RegisterUserCommand command);

    User getUserById(Long id);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUserProfile(Long id, UpdateProfileCommand command);

    User toggleUserStatus(Long id, boolean active);
}
