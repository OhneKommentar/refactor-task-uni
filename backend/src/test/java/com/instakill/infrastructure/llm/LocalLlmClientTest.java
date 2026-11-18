package com.instakill.infrastructure.llm;

import com.instakill.common.error.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LocalLlmClientTest {

    private static final String URL = "http://localhost:12345/v1/chat/completions";

    private MockRestServiceServer server;
    private LocalLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LocalLlmClient(builder.build(),
                new LocalLlmProperties(URL, "google/gemma-3-270m", Duration.ofSeconds(5), "Be funny."));
    }

    @Test
    void sendsChatCompletionRequestAndReturnsTrimmedAnswer() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("google/gemma-3-270m"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("Be funny."))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("I am at the lab."))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.max_tokens").value(256))
                .andExpect(jsonPath("$.stream").value(false))
                .andRespond(withSuccess("""
                        {"id":"chatcmpl-1","object":"chat.completion","model":"google/gemma-3-270m",
                         "choices":[{"index":0,"finish_reason":"stop",
                           "message":{"role":"assistant","content":"  I survived the lab, barely!  "}}],
                         "usage":{"prompt_tokens":10,"completion_tokens":8,"total_tokens":18}}
                        """, MediaType.APPLICATION_JSON));

        String result = client.makeFunnier("I am at the lab.");

        assertThat(result).isEqualTo("I survived the lab, barely!");
        server.verify();
    }

    @Test
    void serverErrorBecomesServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.makeFunnier("hello"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void timeoutOrConnectionProblemBecomesServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.makeFunnier("hello"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("not reachable");
    }

    @Test
    void emptyChoicesBecomeServiceUnavailable() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.makeFunnier("hello"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("empty");
    }
}
