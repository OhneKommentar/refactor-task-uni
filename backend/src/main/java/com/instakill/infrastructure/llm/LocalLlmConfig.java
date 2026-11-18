package com.instakill.infrastructure.llm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LocalLlmConfig {

    @Bean
    public RestClient localLlmRestClient(RestClient.Builder builder, LocalLlmProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        return builder.requestFactory(requestFactory).build();
    }
}
