package com.instakill.api.web;

import com.instakill.user.application.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvcFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // ensure user exists
        try {
            authService.register("webuser", "web@example.com", "password");
        } catch (Exception ignored) {
        }
    }

    @Test
    void loginCreatePostAndViewFeed() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("usernameOrEmail", "webuser")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feed"))
                .andReturn();

        Cookie token = loginResult.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(token).isNotNull();

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .cookie(token)
                        .param("content", "Hello from MVC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feed"));

        mockMvc.perform(get("/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello from MVC")));
    }
}
