package com.instakill.post.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Post(
        UUID id,
        UUID authorId,
        String content,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        long commentCount
) {
    private static final int MAX_CONTENT_LENGTH = 1000;

    public Post {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must be <= " + MAX_CONTENT_LENGTH + " characters");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (likeCount < 0) {
            throw new IllegalArgumentException("likeCount must be >= 0");
        }
        if (commentCount < 0) {
            throw new IllegalArgumentException("commentCount must be >= 0");
        }
    }

    public Post withLikeCount(long newCount) {
        return new Post(id, authorId, content, imageUrl, createdAt, updatedAt, newCount, commentCount);
    }

    public Post withCommentCount(long newCount) {
        return new Post(id, authorId, content, imageUrl, createdAt, updatedAt, likeCount, newCount);
    }

    public Post withUpdatedAt(Instant newUpdatedAt) {
        return new Post(id, authorId, content, imageUrl, createdAt, newUpdatedAt, likeCount, commentCount);
    }
}
