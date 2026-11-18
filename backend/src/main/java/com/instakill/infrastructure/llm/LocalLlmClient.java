package com.instakill.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.instakill.common.error.ServiceUnavailableException;
import com.instakill.funnyfy.application.FunnyTextGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Talks to a local LLM server (e.g. LM Studio) through the OpenAI-compatible /v1/chat/completions endpoint.
 */
@Component
public class LocalLlmClient implements FunnyTextGenerator {

    private static final Logger log = LoggerFactory.getLogger(LocalLlmClient.class);
    private static final double TEMPERATURE = 0.7;
    private static final int MAX_TOKENS = 256;

    private final RestClient restClient;
    private final LocalLlmProperties properties;

    public LocalLlmClient(@Qualifier("localLlmRestClient") RestClient restClient, LocalLlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String makeFunnier(String text) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(new Message("system", properties.systemPrompt()), new Message("user", text)),
                TEMPERATURE,
                MAX_TOKENS,
                false);

        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri(properties.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (ResourceAccessException e) {
            log.warn("Local LLM not reachable at {}: {}", properties.url(), e.getMessage());
            throw new ServiceUnavailableException(
                    "The funny AI is not reachable right now (server down or timed out). Please try again later.", e);
        } catch (RestClientException e) {
            log.warn("Local LLM request failed: {}", e.getMessage());
            throw new ServiceUnavailableException("The funny AI could not process your text. Please try again later.", e);
        }

        String content = extractContent(response);
        if (content == null || content.isBlank()) {
            throw new ServiceUnavailableException("The funny AI returned an empty answer. Please try again.");
        }
        return content.trim();
    }

    private static String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        Choice choice = response.choices().getFirst();
        return choice == null || choice.message() == null ? null : choice.message().content();
    }

    record ChatCompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            boolean stream
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {
    }
}
