package com.instakill.post.api.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 1000) String content,
        @Size(max = 2048) String imageUrl
) {
}
