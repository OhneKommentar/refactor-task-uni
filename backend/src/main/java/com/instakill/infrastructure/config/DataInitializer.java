package com.instakill.infrastructure.config;

import com.instakill.feed.domain.FeedSort;
import com.instakill.interaction.application.InteractionService;
import com.instakill.post.application.PostService;
import com.instakill.post.domain.Post;
import com.instakill.user.application.AuthService;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PostService postService;
    private final InteractionService interactionService;

    public DataInitializer(UserRepository userRepository,
                           AuthService authService,
                           PostService postService,
                           InteractionService interactionService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.postService = postService;
        this.interactionService = interactionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.findAll().isEmpty()) {
            return;
        }
        log.info("Seeding development data");
        User alice = authService.register("alice", "alice@example.com", "password").user();
        User bob = authService.register("bob", "bob@example.com", "password").user();

        List<Post> alicePosts = List.of(
                postService.createPost(alice.id(), "Hello Instakill!", null),
                postService.createPost(alice.id(), "Sunset vibes", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee"),
                postService.createPost(alice.id(), "Weekend adventures", null)
        );

        List<Post> bobPosts = List.of(
                postService.createPost(bob.id(), "Coffee first", "https://images.unsplash.com/photo-1447933601403-0c6688de566e"),
                postService.createPost(bob.id(), "Design inspo", null),
                postService.createPost(bob.id(), "Working on side projects", null)
        );

        interactionService.addComment(alicePosts.getFirst().id(), bob.id(), "Welcome Alice!");
        interactionService.addComment(alicePosts.get(1).id(), bob.id(), "Gorgeous view");
        interactionService.addComment(bobPosts.getFirst().id(), alice.id(), "Save me a cup!");
        interactionService.addComment(bobPosts.get(1).id(), alice.id(), "Love this palette");

        interactionService.toggleLike(alicePosts.getFirst().id(), bob.id());
        interactionService.toggleLike(alicePosts.get(1).id(), bob.id());
        interactionService.toggleLike(bobPosts.getFirst().id(), alice.id());
        interactionService.toggleLike(bobPosts.get(2).id(), alice.id());

        postService.listFeed(FeedSort.NEW, 0, 10);
    }
}
