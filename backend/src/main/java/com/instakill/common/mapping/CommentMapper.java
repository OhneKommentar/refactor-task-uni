package com.instakill.common.mapping;

import com.instakill.infrastructure.persistence.entity.CommentEntity;
import com.instakill.interaction.api.rest.CommentResponse;
import com.instakill.interaction.domain.Comment;
import com.instakill.user.domain.User;

public final class CommentMapper {
    private CommentMapper() {
    }

    public static CommentEntity toEntity(Comment comment) {
        CommentEntity entity = new CommentEntity();
        entity.setId(comment.id());
        entity.setPostId(comment.postId());
        entity.setAuthorId(comment.authorId());
        entity.setContent(comment.content());
        entity.setCreatedAt(comment.createdAt());
        return entity;
    }

    public static Comment toDomain(CommentEntity entity) {
        return new Comment(
                entity.getId(),
                entity.getPostId(),
                entity.getAuthorId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public static CommentResponse toResponse(Comment comment, User author) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.authorId(),
                comment.content(),
                comment.createdAt(),
                UserMapper.toResponse(author)
        );
    }
}
