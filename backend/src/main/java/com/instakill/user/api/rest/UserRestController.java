package com.instakill.user.api.rest;

import com.instakill.common.mapping.UserMapper;
import com.instakill.infrastructure.security.JwtUserPrincipal;
import com.instakill.user.application.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return UserMapper.toResponse(userService.getById(principal.id()));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return UserMapper.toResponse(userService.getById(id));
    }
}
