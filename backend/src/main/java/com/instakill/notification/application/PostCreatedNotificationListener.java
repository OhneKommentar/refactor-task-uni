package com.instakill.notification.application;

import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.application.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PostCreatedNotificationListener {

    private final NotificationSender sender;
    private final UserService userService;

    public PostCreatedNotificationListener(NotificationSender sender, UserService userService) {
        this.sender = sender;
        this.userService = userService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        String username = userService.getById(event.authorId()).username();
        sender.send(
                "New post by @" + username,
                "Post " + event.postId() + " created at " + event.createdAt()
        );
    }
}
