package com.instakill.infrastructure.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID id, String username) {
}
