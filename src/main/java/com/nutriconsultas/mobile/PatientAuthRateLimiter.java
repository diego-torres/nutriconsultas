package com.nutriconsultas.mobile;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@Component
public final class PatientAuthRateLimiter {

	public static final String PATIENT_AUTH = "patientAuth";

	private final RateLimiterRegistry rateLimiterRegistry;

	public PatientAuthRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T execute(final String clientKey, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(PATIENT_AUTH);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(PATIENT_AUTH + ":" + clientKey, config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (final RequestNotPermitted ex) {
			throw ex;
		}
		catch (final Exception ex) {
			throw new IllegalStateException("Rate-limited patient auth call failed", ex);
		}
	}

}
