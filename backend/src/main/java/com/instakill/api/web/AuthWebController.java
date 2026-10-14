package com.instakill.api.web;

import com.instakill.common.error.InstakillException;
import com.instakill.infrastructure.security.JwtAuthenticationFilter;
import com.instakill.infrastructure.security.JwtProperties;
import com.instakill.user.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class AuthWebController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthWebController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "auth_login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult bindingResult,
                        HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "auth_login";
        }
        try {
            AuthService.AuthResult result = authService.login(form.getUsernameOrEmail(), form.getPassword());
            addAuthCookie(response, result.token());
            return "redirect:/feed";
        } catch (InstakillException ex) {
            bindingResult.reject("login.failed", ex.getMessage());
            return "auth_login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth_register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "auth_register";
        }
        try {
            AuthService.AuthResult result = authService.register(form.getUsername(), form.getEmail(), form.getPassword());
            addAuthCookie(response, result.token());
            return "redirect:/feed";
        } catch (InstakillException ex) {
            bindingResult.reject("register.failed", ex.getMessage());
            return "auth_register";
        }
    }

    private void addAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.ttlMinutes() * 60)
                .secure(false)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
