package com.instakill.infrastructure.persistence.repository;

import com.instakill.infrastructure.persistence.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaLikeRepository extends JpaRepository<LikeEntity, UUID> {

    Optional<LikeEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}
