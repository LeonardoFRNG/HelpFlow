package com.helpflow.ticket.application.service;

import com.helpflow.ticket.application.port.in.UserManagementUseCase;
import com.helpflow.ticket.application.port.out.PasswordEncoderPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.UserNotFoundException;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserApplicationService implements UserManagementUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public UserApplicationService(UserRepositoryPort userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegisterUserCommand command) {
        if (userRepository.existsByCorporateEmail(command.corporateEmail())) {
            throw new BusinessRuleException("A user with corporate email " + command.corporateEmail() + " already exists");
        }
        if (userRepository.existsByDocument(command.document())) {
            throw new BusinessRuleException("A user with document " + command.document() + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(command.password());
        Role userRole = command.role() != null ? command.role() : Role.ROLE_USUARIO;

        User user = User.createNew(
                command.document(),
                command.name(),
                command.corporateEmail(),
                encodedPassword,
                command.location(),
                command.area(),
                userRole
        );

        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByCorporateEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUserProfile(Long id, UpdateProfileCommand command) {
        User user = getUserById(id);
        user.updateProfile(command.name(), command.location(), command.area());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User toggleUserStatus(Long id, boolean active) {
        User user = getUserById(id);
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return userRepository.save(user);
    }
}
