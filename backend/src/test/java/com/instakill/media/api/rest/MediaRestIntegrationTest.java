package com.instakill.media.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaRestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadedImageIsScaledAndReturnedUnderMediaUrl() throws Exception {
        Cookie token = registerUser("mediauser");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, pngBytes(800, 400));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/media")
                        .file(file)
                        .cookie(token))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String mediaUrl = uploadJson.get("mediaUrl").asText();
        assertThat(mediaUrl).startsWith("/media/").endsWith(".png");

        MvcResult served = mockMvc.perform(get(mediaUrl))
                .andExpect(status().isOk())
                .andReturn();

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(served.getResponse().getContentAsByteArray()));
        assertThat(stored.getWidth()).isEqualTo(300);
        assertThat(stored.getHeight()).isEqualTo(150);
    }

    @Test
    void invalidImageIsRejected() throws Exception {
        Cookie token = registerUser("badimageuser");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "fake png bytes".getBytes());

        mockMvc.perform(multipart("/api/media")
                        .file(file)
                        .cookie(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, pngBytes(100, 50));

        mockMvc.perform(multipart("/api/media").file(file))
                .andExpect(status().isUnauthorized());
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

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private record AuthPayload(String username, String email, String password) {
    }
}
