package com.nutriconsultas.ai;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

/**
 * Per-nutritionist rate limiting for AI chat message orchestration (#386).
 */
@Component
public final class AiChatRateLimiter {

	public static final String AI_CHAT_MESSAGE = "aiChatMessage";

	public static final String RATE_LIMIT_USER_MESSAGE = "Has alcanzado el límite de mensajes del asistente de IA. "
			+ "Intenta de nuevo en unos minutos.";

	private final RateLimiterRegistry rateLimiterRegistry;

	public AiChatRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T executeMessage(final String nutritionistId, final Callable<T> callable) {
		return execute(AI_CHAT_MESSAGE, nutritionistId, callable);
	}

	private <T> T execute(final String instanceName, final String nutritionistId, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(instanceName);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(instanceName + ":" + nutritionistId, config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (RequestNotPermitted | RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Rate-limited AI chat call failed", ex);
		}
	}

}
