package com.instakill.common.mapping;

import com.instakill.infrastructure.persistence.entity.LikeEntity;
import com.instakill.interaction.domain.Like;

public final class LikeMapper {
    private LikeMapper() {
    }

    public static LikeEntity toEntity(Like like) {
        LikeEntity entity = new LikeEntity();
        entity.setId(like.id());
        entity.setPostId(like.postId());
        entity.setUserId(like.userId());
        entity.setCreatedAt(like.createdAt());
        return entity;
    }

    public static Like toDomain(LikeEntity entity) {
        return new Like(entity.getId(), entity.getPostId(), entity.getUserId(), entity.getCreatedAt());
    }
}
