package com.nutriconsultas.ai;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfig {

	@Bean
	RestClient openAiRestClient(final AiProperties properties) {
		final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(properties.getOpenai().getConnectTimeoutMs()));
		requestFactory.setReadTimeout(Duration.ofMillis(properties.getOpenai().getReadTimeoutMs()));
		return RestClient.builder().baseUrl(properties.getOpenai().getBaseUrl()).requestFactory(requestFactory).build();
	}

}
