package com.instakill.infrastructure.notification;

import com.instakill.notification.application.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NtfyNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NtfyNotificationSender.class);

    private final RestClient restClient;
    private final NtfyProperties properties;

    public NtfyNotificationSender(RestClient.Builder builder, NtfyProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public void send(String title, String message) {
        if (!properties.enabled()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/{topic}", properties.topic())
                    .header("Title", title)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Could not send ntfy notification: {}", e.getMessage());
        }
    }
}
