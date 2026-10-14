package com.instakill.user.api.rest;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, String email, Instant createdAt) {
}
