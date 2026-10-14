package com.instakill.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullRestFlow() throws Exception {
        String registerPayload = objectMapper.writeValueAsString(new AuthPayload("restuser", "rest@example.com", "password"));
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        Cookie token = registerResult.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(token).isNotNull();
        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        assertThat(registerJson.get("user").get("username").asText()).isEqualTo("restuser");

        String createPostPayload = "{" +
                "\"content\":\"Integration content\"," +
                "\"imageUrl\":null" +
                "}";
        MvcResult postResult = mockMvc.perform(post("/api/posts")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode postJson = objectMapper.readTree(postResult.getResponse().getContentAsString());
        UUID postId = UUID.fromString(postJson.get("id").asText());

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Nice work!\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts/" + postId + "/likes/toggle")
                        .cookie(token))
                .andExpect(status().isOk());

        MvcResult feedResult = mockMvc.perform(get("/api/feed")
                        .cookie(token)
                        .param("sort", "new"))
                .andExpect(status().isOk())
                .andReturn();

        String feedBody = feedResult.getResponse().getContentAsString();
        JsonNode feedJson = objectMapper.readTree(feedBody);
        assertThat(feedJson.isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : feedJson) {
            if (node.get("content").asText().contains("Integration content")) {
                found = true;
                break;
            }
        }
        assertThat(found).as(feedBody).isTrue();
    }

    private record AuthPayload(String username, String email, String password) {
    }
}
