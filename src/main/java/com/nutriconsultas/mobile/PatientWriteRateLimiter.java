package com.nutriconsultas.mobile;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

/**
 * Per-patient rate limiting for mobile write endpoints (#113). Each patient
 * ({@code patientAuthSub}) gets an isolated limiter instance keyed off the configured
 * Resilience4j instance name.
 */
@Component
public final class PatientWriteRateLimiter {

	public static final String PATIENT_MESSAGES = "patientMessages";

	private final RateLimiterRegistry rateLimiterRegistry;

	public PatientWriteRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T execute(final String instanceName, final String patientAuthSub, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(instanceName);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(instanceName + ":" + patientAuthSub, config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (RequestNotPermitted ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Rate-limited call failed", ex);
		}
	}

}
