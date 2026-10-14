package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TrendingFeedRankingStrategy implements FeedRankingStrategy {

    @Override
    public FeedSort sort() {
        return FeedSort.TRENDING;
    }

    @Override
    public List<Post> fetch(PostRepository repository, int page, int size) {
        // Fetch more posts than needed to allow for proper trending sorting
        int fetchSize = Math.max(size * 2, Math.max(size, 10));
        List<Post> candidates = repository.findLatest(0, (page + 1) * fetchSize);

        // Sort by likes (descending), then comments (descending), then recency (descending)
        Comparator<Post> comparator = Comparator
                .comparingLong(Post::likeCount).reversed()
                .thenComparing(Comparator.comparingLong(Post::commentCount).reversed())
                .thenComparing(Comparator.comparing(Post::createdAt).reversed());

        return candidates.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}
