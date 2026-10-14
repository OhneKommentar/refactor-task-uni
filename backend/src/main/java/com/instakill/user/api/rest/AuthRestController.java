package com.instakill.user.api.rest;

import com.instakill.common.mapping.UserMapper;
import com.instakill.infrastructure.security.JwtAuthenticationFilter;
import com.instakill.infrastructure.security.JwtProperties;
import com.instakill.user.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthRestController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request.username(), request.email(), request.password());
        return buildAuthResponse(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.usernameOrEmail(), request.password());
        return buildAuthResponse(result);
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(AuthService.AuthResult result) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, result.token())
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.ttlMinutes() * 60)
                .secure(false)
                .sameSite("Lax")
                .build();
        AuthResponse response = new AuthResponse(result.token(), UserMapper.toResponse(result.user()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
