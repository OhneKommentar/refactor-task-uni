package com.instakill.common.mapping;

import com.instakill.infrastructure.persistence.entity.UserEntity;
import com.instakill.user.api.rest.UserResponse;
import com.instakill.user.domain.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setCreatedAt(user.createdAt());
        return entity;
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.username(), user.email(), user.createdAt());
    }
}
