package com.instakill.notification.application;

import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.application.UserService;
import com.instakill.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostCreatedNotificationListenerTest {

    @Test
    void sendsNotificationWithUsernamePostIdAndTimestamp() {
        NotificationSender sender = mock(NotificationSender.class);
        UserService userService = mock(UserService.class);
        PostCreatedNotificationListener listener = new PostCreatedNotificationListener(sender, userService);

        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-28T10:00:00Z");
        when(userService.getById(authorId))
                .thenReturn(new User(authorId, "alice", "alice@example.com", "hash", createdAt));

        listener.onPostCreated(new PostCreatedEvent(postId, authorId, createdAt));

        verify(sender).send(
                "New post by @alice",
                "Post " + postId + " created at " + createdAt
        );
    }
}
