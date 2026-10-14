package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewestFeedRankingStrategy implements FeedRankingStrategy {

    @Override
    public FeedSort sort() {
        return FeedSort.NEW;
    }

    @Override
    public List<Post> fetch(PostRepository repository, int page, int size) {
        return repository.findLatest(page, size);
    }
}
