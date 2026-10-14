package com.instakill.interaction.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Like(
        UUID id,
        UUID postId,
        UUID userId,
        Instant createdAt
) {
    public Like {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
