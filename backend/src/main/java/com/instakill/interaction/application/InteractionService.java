package com.instakill.interaction.application;

import com.instakill.common.clock.Clock;
import com.instakill.common.error.NotFoundException;
import com.instakill.common.id.IdGenerator;
import com.instakill.interaction.domain.Comment;
import com.instakill.interaction.domain.CommentRepository;
import com.instakill.interaction.domain.DomainEventPublisher;
import com.instakill.interaction.domain.Like;
import com.instakill.interaction.domain.LikeCreatedEvent;
import com.instakill.interaction.domain.LikeRepository;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InteractionService {

    private final CommentRepository comments;
    private final LikeRepository likes;
    private final PostRepository posts;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public InteractionService(CommentRepository comments,
                              LikeRepository likes,
                              PostRepository posts,
                              IdGenerator idGenerator,
                              Clock clock,
                              DomainEventPublisher eventPublisher) {
        this.comments = comments;
        this.likes = likes;
        this.posts = posts;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Comment addComment(UUID postId, UUID authorId, String content) {
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        Instant now = clock.now();
        Comment comment = new Comment(idGenerator.generate(), postId, authorId, content, now);
        Comment saved = comments.save(comment);
        long commentCount = comments.countByPostId(postId);
        posts.save(post.withCommentCount(commentCount).withUpdatedAt(now));
        return saved;
    }

    public List<Comment> listComments(UUID postId, int page, int size) {
        posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        return comments.findByPostId(postId, page, size);
    }

    @Transactional
    public ToggleLikeResult toggleLike(UUID postId, UUID userId) {
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        Instant now = clock.now();
        return likes.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    likes.deleteById(existing.id());
                    long likeCount = likes.countByPostId(postId);
                    posts.save(post.withLikeCount(likeCount).withUpdatedAt(now));
                    return new ToggleLikeResult(false, likeCount);
                })
                .orElseGet(() -> {
                    Like like = new Like(idGenerator.generate(), postId, userId, now);
                    likes.save(like);
                    long likeCount = likes.countByPostId(postId);
                    posts.save(post.withLikeCount(likeCount).withUpdatedAt(now));
                eventPublisher.publish(new LikeCreatedEvent(postId, userId, now));
                    return new ToggleLikeResult(true, likeCount);
                });
    }

    public boolean hasUserLiked(UUID postId, UUID userId) {
        return likes.existsByPostIdAndUserId(postId, userId);
    }

    public record ToggleLikeResult(boolean liked, long likeCount) {
    }
}
