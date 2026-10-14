package com.instakill.infrastructure.security;

import com.instakill.common.clock.Clock;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenService {

    private final JwtProperties properties;
    private final Clock clock;
    private javax.crypto.SecretKey key;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(UUID userId, String username) {
        Instant now = clock.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.ttlMinutes()));
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<JwtUserPrincipal> parseToken(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = parsed.getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            return Optional.of(new JwtUserPrincipal(userId, username));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
