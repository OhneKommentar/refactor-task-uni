package com.instakill.infrastructure.persistence.repository;

import com.instakill.infrastructure.persistence.entity.CommentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);
}
