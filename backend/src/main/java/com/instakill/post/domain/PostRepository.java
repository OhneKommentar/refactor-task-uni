package com.instakill.post.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(UUID id);

    void deleteById(UUID id);

    List<Post> findLatest(int page, int size);

    List<Post> findTrending(int page, int size);

    long countByAuthor(UUID authorId);
}
