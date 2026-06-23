package com.nutriconsultas.mobile;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

/**
 * Rate limits authenticated patient invitation redeem requests (#136).
 */
@Component
public final class PatientInvitationRedeemRateLimiter {

	public static final String PATIENT_INVITATION_REDEEM = "patientInvitationRedeem";

	private final RateLimiterRegistry rateLimiterRegistry;

	public PatientInvitationRedeemRateLimiter(final RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	public <T> T execute(final String patientAuthSub, final Callable<T> callable) {
		final RateLimiter templateLimiter = rateLimiterRegistry.rateLimiter(PATIENT_INVITATION_REDEEM);
		final RateLimiterConfig config = templateLimiter.getRateLimiterConfig();
		final RateLimiter limiter = rateLimiterRegistry.rateLimiter(PATIENT_INVITATION_REDEEM + ":" + patientAuthSub,
				config);
		try {
			return RateLimiter.decorateCallable(limiter, callable).call();
		}
		catch (RequestNotPermitted ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Rate-limited invitation redeem call failed", ex);
		}
	}

}
