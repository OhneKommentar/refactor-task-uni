package com.instakill.feed.application;

import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

class FeedServiceTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);
    private final FeedService service = new FeedService(repository,
            List.of(new NewestFeedRankingStrategy(), new TrendingFeedRankingStrategy()));

    @Test
    void sortsTrendingFeedByLikesThenCommentsThenRecency() {
        Instant now = Instant.now();
        List<Post> posts = List.of(
                post(now.minusSeconds(60), 2, 5),
                post(now.minusSeconds(30), 5, 1),
                post(now.minusSeconds(10), 5, 3)
        );
        when(repository.findLatest(0, 10)).thenReturn(posts);

        List<Post> result = service.listFeed(FeedSort.TRENDING, 0, 2);

        assertThat(result)
                .hasSize(2)
                .extracting(Post::likeCount, Post::commentCount)
                .containsExactly(
                        tuple(5L, 3L),
                        tuple(5L, 1L)
                );
    }
    
    @Test
    void returnsNewestFeedInCorrectOrder() {
        List<Post> posts = List.of(
                post(Instant.now().minusSeconds(10), 1, 1),
                post(Instant.now().minusSeconds(20), 2, 2)
        );
        when(repository.findLatest(0, 2)).thenReturn(posts);

        List<Post> result = service.listFeed(FeedSort.NEW, 0, 2);

        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(posts);
    }
    
    @Test
    void defaultsToNewWhenSortIsNull() {
        List<Post> posts = List.of(post(Instant.now(), 1, 1));
        when(repository.findLatest(0, 5)).thenReturn(posts);

        List<Post> result = service.listFeed(null, 0, 5);

        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(posts);
    }

    private Post post(Instant createdAt, long likes, long comments) {
        UUID id = UUID.randomUUID();
        return new Post(id, UUID.randomUUID(), "content", null, createdAt, createdAt, likes, comments);
    }
}
