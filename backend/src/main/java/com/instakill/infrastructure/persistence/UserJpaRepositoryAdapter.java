package com.instakill.infrastructure.persistence;

import com.instakill.common.mapping.UserMapper;
import com.instakill.infrastructure.persistence.entity.UserEntity;
import com.instakill.infrastructure.persistence.repository.JpaUserRepository;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class UserJpaRepositoryAdapter implements UserRepository {

    private final JpaUserRepository repository;

    public UserJpaRepositoryAdapter(JpaUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return repository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = repository.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(entity);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(UserMapper::toDomain).toList();
    }
}
