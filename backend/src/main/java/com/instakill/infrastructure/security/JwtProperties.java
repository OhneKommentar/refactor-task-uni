package com.instakill.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instakill.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long ttlMinutes
) {
}
