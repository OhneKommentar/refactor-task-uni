package com.instakill.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "instakill.llm")
public record LocalLlmProperties(
        String url,
        String model,
        Duration timeout,
        String systemPrompt
) {
}
