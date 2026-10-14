package com.instakill.interaction.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Comment(
        UUID id,
        UUID postId,
        UUID authorId,
        String content,
        Instant createdAt
) {
    private static final int MAX_CONTENT_LENGTH = 500;

    public Comment {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must be <= " + MAX_CONTENT_LENGTH + " characters");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
