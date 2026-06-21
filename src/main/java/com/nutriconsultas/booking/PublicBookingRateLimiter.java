package com.nutriconsultas.booking;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

/**
 * Rate limits anonymous public booking submissions (#248).
 */
@Component
public final class PublicBookingRateLimiter {

	public static final String PUBLIC_BOOKING = "publicBooking";

	private final RateLimiterRegistry rateLimiterRegistry;

	public PublicBookingRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T execute(final String clientKey, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(PUBLIC_BOOKING);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(PUBLIC_BOOKING + ":" + clientKey, config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (RequestNotPermitted ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Rate-limited public booking call failed", ex);
		}
	}

}
