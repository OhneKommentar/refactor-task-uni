package com.instakill.infrastructure.persistence.repository;

import com.instakill.infrastructure.persistence.entity.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JpaPostRepository extends JpaRepository<PostEntity, UUID> {

    List<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM PostEntity p ORDER BY p.likeCount DESC, p.commentCount DESC, p.createdAt DESC")
    List<PostEntity> findTrending(Pageable pageable);

    long countByAuthorId(UUID authorId);
}
