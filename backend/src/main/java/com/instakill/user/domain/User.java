package com.instakill.user.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String email,
        String passwordHash,
        Instant createdAt
) {
    public User {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }
}
