package com.instakill.common.mapping;

import com.instakill.infrastructure.persistence.entity.PostEntity;
import com.instakill.post.domain.Post;
import com.instakill.user.domain.User;
import com.instakill.post.api.rest.PostResponse;

public final class PostMapper {
    private PostMapper() {
    }

    public static PostEntity toEntity(Post post) {
        PostEntity entity = new PostEntity();
        entity.setId(post.id());
        entity.setAuthorId(post.authorId());
        entity.setContent(post.content());
        entity.setImageUrl(post.imageUrl());
        entity.setCreatedAt(post.createdAt());
        entity.setUpdatedAt(post.updatedAt());
        entity.setLikeCount(post.likeCount());
        entity.setCommentCount(post.commentCount());
        return entity;
    }

    public static Post toDomain(PostEntity entity) {
        return new Post(
                entity.getId(),
                entity.getAuthorId(),
                entity.getContent(),
                entity.getImageUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLikeCount(),
                entity.getCommentCount()
        );
    }

    public static PostResponse toResponse(Post post, User author, boolean likedByCurrentUser) {
        return new PostResponse(
                post.id(),
                post.authorId(),
                post.content(),
                post.imageUrl(),
                post.createdAt(),
                post.updatedAt(),
                post.likeCount(),
                post.commentCount(),
                likedByCurrentUser,
                UserMapper.toResponse(author)
        );
    }
}
