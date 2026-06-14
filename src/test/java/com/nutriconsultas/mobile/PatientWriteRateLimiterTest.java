package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

class PatientWriteRateLimiterTest {

	private static final String INSTANCE = PatientWriteRateLimiter.PATIENT_MESSAGES;

	private static final String PATIENT_SUB = "auth0|rate-limit-patient";

	@Test
	void executeRunsCallableWhenPermitAvailable() throws Exception {
		final RateLimiterRegistry registry = registryWithLimit(1);
		final PatientWriteRateLimiter liveLimiter = new PatientWriteRateLimiter(registry);

		final String result = liveLimiter.execute(INSTANCE, PATIENT_SUB, () -> "ok");

		assertThat(result).isEqualTo("ok");
	}

	@Test
	void executeUsesPerPatientKey() throws Exception {
		final RateLimiterRegistry registry = registryWithLimit(1);
		final PatientWriteRateLimiter liveLimiter = new PatientWriteRateLimiter(registry);

		liveLimiter.execute(INSTANCE, PATIENT_SUB, () -> "first");
		assertThatThrownBy(() -> liveLimiter.execute(INSTANCE, PATIENT_SUB, () -> "second"))
			.isInstanceOf(RequestNotPermitted.class);

		final String otherPatientResult = liveLimiter.execute(INSTANCE, "auth0|other-patient", () -> "other");
		assertThat(otherPatientResult).isEqualTo("other");
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
