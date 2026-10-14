package com.instakill.interaction.domain;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository {

    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);

    Like save(Like like);

    void deleteById(UUID id);

    long countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}
