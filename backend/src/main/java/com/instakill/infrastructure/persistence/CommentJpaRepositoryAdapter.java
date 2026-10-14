package com.instakill.infrastructure.persistence;

import com.instakill.common.mapping.CommentMapper;
import com.instakill.infrastructure.persistence.repository.JpaCommentRepository;
import com.instakill.interaction.domain.Comment;
import com.instakill.interaction.domain.CommentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class CommentJpaRepositoryAdapter implements CommentRepository {

    private final JpaCommentRepository repository;

    public CommentJpaRepositoryAdapter(JpaCommentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Comment save(Comment comment) {
        return CommentMapper.toDomain(repository.save(CommentMapper.toEntity(comment)));
    }

    @Override
    public List<Comment> findByPostId(UUID postId, int page, int size) {
        return repository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(page, size)).stream()
                .map(CommentMapper::toDomain)
                .toList();
    }

    @Override
    public long countByPostId(UUID postId) {
        return repository.countByPostId(postId);
    }
}
