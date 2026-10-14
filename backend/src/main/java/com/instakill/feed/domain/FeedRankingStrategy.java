package com.instakill.feed.domain;

import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;

import java.util.List;

public interface FeedRankingStrategy {

    FeedSort sort();

    List<Post> fetch(PostRepository repository, int page, int size);
}
