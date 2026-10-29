package com.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.model.User;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SpringDataUserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(SpringDataUserJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByCorporateEmail(String corporateEmail) {
        return repository.findByCorporateEmail(corporateEmail).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByDocument(String document) {
        return repository.findByDocument(document).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCorporateEmail(String corporateEmail) {
        return repository.existsByCorporateEmail(corporateEmail);
    }

    @Override
    public boolean existsByDocument(String document) {
        return repository.existsByDocument(document);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
