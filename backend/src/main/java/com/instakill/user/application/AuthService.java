package com.instakill.user.application;

import com.instakill.common.clock.Clock;
import com.instakill.common.error.ConflictException;
import com.instakill.common.error.NotFoundException;
import com.instakill.common.error.UnauthorizedException;
import com.instakill.common.id.IdGenerator;
import com.instakill.infrastructure.security.JwtTokenService;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final JwtTokenService tokenService;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       IdGenerator idGenerator,
                       Clock clock,
                       JwtTokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResult register(String username, String email, String password) {
        if (users.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        Instant now = clock.now();
        User user = new User(
                idGenerator.generate(),
                username,
                email,
                passwordEncoder.encode(password),
                now
        );
        User saved = users.save(user);
        String token = tokenService.createToken(saved.id(), saved.username());
        return new AuthResult(token, saved);
    }

    public AuthResult login(String usernameOrEmail, String password) {
        User user = users.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = tokenService.createToken(user.id(), user.username());
        return new AuthResult(token, user);
    }

    public record AuthResult(String token, User user) {
    }
}
