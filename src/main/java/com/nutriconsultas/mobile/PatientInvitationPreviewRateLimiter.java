package com.nutriconsultas.mobile;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

/**
 * Rate limits anonymous patient invitation preview requests (#135).
 */
@Component
public final class PatientInvitationPreviewRateLimiter {

	public static final String PATIENT_INVITATION_PREVIEW = "patientInvitationPreview";

	private final RateLimiterRegistry rateLimiterRegistry;

	public PatientInvitationPreviewRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T execute(final String clientKey, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(PATIENT_INVITATION_PREVIEW);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(PATIENT_INVITATION_PREVIEW + ":" + clientKey,
				config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (RequestNotPermitted ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Rate-limited invitation preview call failed", ex);
		}
	}

}
