package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

class AiChatRateLimiterTest {

	private static final String NUTRITIONIST_ID = "auth0|rate-limit-nutritionist";

	@Test
	void executeMessageRunsCallableWhenPermitAvailable() throws Exception {
		final RateLimiterRegistry registry = registryWithLimit(1);
		final AiChatRateLimiter liveLimiter = new AiChatRateLimiter(registry);

		final String result = liveLimiter.executeMessage(NUTRITIONIST_ID, () -> "ok");

		assertThat(result).isEqualTo("ok");
	}

	@Test
	void executeMessageUsesPerNutritionistKey() throws Exception {
		final RateLimiterRegistry registry = registryWithLimit(1);
		final AiChatRateLimiter liveLimiter = new AiChatRateLimiter(registry);

		liveLimiter.executeMessage(NUTRITIONIST_ID, () -> "first");
		assertThatThrownBy(() -> liveLimiter.executeMessage(NUTRITIONIST_ID, () -> "second"))
			.isInstanceOf(RequestNotPermitted.class);

		final String otherResult = liveLimiter.executeMessage("auth0|other-nutritionist", () -> "other");
		assertThat(otherResult).isEqualTo("other");
	}

	@Test
	void executeMessagePropagatesOpenAiClientException() {
		final RateLimiterRegistry registry = registryWithLimit(5);
		final AiChatRateLimiter liveLimiter = new AiChatRateLimiter(registry);
		final OpenAiClientException openAiEx = new OpenAiClientException(OpenAiClientException.ErrorKind.RATE_LIMIT,
				HttpStatus.TOO_MANY_REQUESTS, "El servicio de IA está saturado. Intenta de nuevo en unos minutos.",
				"OpenAI rate limit status=429", null);

		assertThatThrownBy(() -> liveLimiter.executeMessage(NUTRITIONIST_ID, () -> {
			throw openAiEx;
		})).isSameAs(openAiEx);
	}

	private static RateLimiterRegistry registryWithLimit(final int limitForPeriod) {
		final RateLimiterConfig config = RateLimiterConfig.custom()
			.limitForPeriod(limitForPeriod)
			.limitRefreshPeriod(Duration.ofMinutes(1))
			.timeoutDuration(Duration.ZERO)
			.build();
		return RateLimiterRegistry.of(config);
	}

}
