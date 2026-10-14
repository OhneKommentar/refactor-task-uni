package com.instakill.interaction.api.rest;

import com.instakill.user.api.rest.UserResponse;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String content,
        Instant createdAt,
        UserResponse author
) {
}
