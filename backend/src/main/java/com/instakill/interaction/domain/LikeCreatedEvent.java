package com.instakill.interaction.domain;

import java.time.Instant;
import java.util.UUID;

public record LikeCreatedEvent(UUID postId, UUID userId, Instant occurredAt) {
    public LikeCreatedEvent {
        if (postId == null || userId == null || occurredAt == null) {
            throw new IllegalArgumentException("postId, userId and occurredAt must not be null");
        }
    }
}
