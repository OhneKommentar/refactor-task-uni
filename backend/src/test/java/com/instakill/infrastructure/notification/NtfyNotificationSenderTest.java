package com.instakill.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NtfyNotificationSenderTest {

    @Test
    void postsMessageToTopicWithTitleHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ntfy.sh/instakill-demo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Title", "New post by @alice"))
                .andExpect(content().string("Post 1 created"))
                .andRespond(withSuccess());
        NtfyNotificationSender sender = new NtfyNotificationSender(builder,
                new NtfyProperties(true, "https://ntfy.sh", "instakill-demo"));

        sender.send("New post by @alice", "Post 1 created");

        server.verify();
    }

    @Test
    void swallowsServerErrors() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ntfy.sh/instakill-demo"))
                .andRespond(withServerError());
        NtfyNotificationSender sender = new NtfyNotificationSender(builder,
                new NtfyProperties(true, "https://ntfy.sh", "instakill-demo"));

        sender.send("New post by @alice", "Post 1 created");

        server.verify();
    }

    @Test
    void sendsNothingWhenDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NtfyNotificationSender sender = new NtfyNotificationSender(builder,
                new NtfyProperties(false, "https://ntfy.sh", "instakill-demo"));

        sender.send("New post by @alice", "Post 1 created");

        server.verify();
    }
}
