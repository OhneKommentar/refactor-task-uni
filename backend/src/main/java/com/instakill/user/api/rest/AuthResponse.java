package com.instakill.user.api.rest;

public record AuthResponse(String token, UserResponse user) {
}
