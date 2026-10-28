package com.instakill.infrastructure.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instakill.notification.ntfy")
public record NtfyProperties(
        boolean enabled,
        String baseUrl,
        String topic
) {
}
