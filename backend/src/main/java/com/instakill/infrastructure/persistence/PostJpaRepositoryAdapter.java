package com.instakill.infrastructure.persistence;

import com.instakill.common.mapping.PostMapper;
import com.instakill.infrastructure.persistence.repository.JpaPostRepository;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class PostJpaRepositoryAdapter implements PostRepository {

    private final JpaPostRepository repository;

    public PostJpaRepositoryAdapter(JpaPostRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Post save(Post post) {
        return PostMapper.toDomain(repository.save(PostMapper.toEntity(post)));
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return repository.findById(id).map(PostMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Post> findLatest(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    @Override
    public List<Post> findTrending(int page, int size) {
        return repository.findTrending(PageRequest.of(page, size)).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    @Override
    public long countByAuthor(UUID authorId) {
        return repository.countByAuthorId(authorId);
    }
}
