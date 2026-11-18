package com.instakill.funnyfy.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instakill.common.error.ServiceUnavailableException;
import com.instakill.funnyfy.application.FunnyTextGenerator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FunnyfyRestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FunnyTextGenerator funnyTextGenerator;

    @Test
    void returnsFunnierText() throws Exception {
        Cookie token = registerUser("funnyuser");
        when(funnyTextGenerator.makeFunnier("Today I'm at my 7th lab."))
                .thenReturn("Lab number 7 and I'm still not a robot!");

        mockMvc.perform(post("/api/funnyfy")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Today I'm at my 7th lab.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Lab number 7 and I'm still not a robot!"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/funnyfy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(funnyTextGenerator);
    }

    @Test
    void rejectsBlankText() throws Exception {
        Cookie token = registerUser("blankfunnyuser");

        mockMvc.perform(post("/api/funnyfy")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(funnyTextGenerator);
    }

    @Test
    void returnsServiceUnavailableWhenLlmIsDown() throws Exception {
        Cookie token = registerUser("downfunnyuser");
        when(funnyTextGenerator.makeFunnier(anyString()))
                .thenThrow(new ServiceUnavailableException("The funny AI is not reachable right now."));

        mockMvc.perform(post("/api/funnyfy")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("The funny AI is not reachable right now."));
    }

    private Cookie registerUser(String username) throws Exception {
        String payload = objectMapper.writeValueAsString(new AuthPayload(username, username + "@example.com", "password"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("ACCESS_TOKEN");
    }

    private record AuthPayload(String username, String email, String password) {
    }
}
