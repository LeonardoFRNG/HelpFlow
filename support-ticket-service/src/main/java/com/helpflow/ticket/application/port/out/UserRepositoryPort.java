package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByCorporateEmail(String corporateEmail);

    Optional<User> findByDocument(String document);

    boolean existsByCorporateEmail(String corporateEmail);

    boolean existsByDocument(String document);

    List<User> findAll();
}
