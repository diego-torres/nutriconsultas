package com.nutriconsultas.auth.apple;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Shared helpers for Apple Sign-In integration tests (#509). Uses locally generated RSA
 * keys — no live Apple or Auth0 credentials.
 */
public final class AppleSignInIntegrationTestSupport {

	public static final String WEBHOOK_URL = "/rest/webhooks/apple/sign-in";

	public static final String AUDIENCE = "com.minutriporcion.app";

	public static final String ISSUER = "https://appleid.apple.com";

	public static final String APPLE_SUBJECT = "001234.abc";

	public static final String AUTH0_USER_ID = "apple|001234.abc";

	private static final RSAKey RSA_KEY;

	static {
		try {
			RSA_KEY = AppleSignInTestJwtSupport.generateRsaKey();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Unable to generate Apple Sign-In test RSA key", ex);
		}
	}

	private AppleSignInIntegrationTestSupport() {
	}

	public static String signNotification(final String eventId, final String eventTypeKey,
			final Map<String, Object> eventPayload) {
		try {
			final Instant issuedAt = Instant.now();
			final Instant expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);
			return AppleSignInTestJwtSupport.signNotification(new AppleSignInTestJwtSupport.SignNotificationRequest(
					RSA_KEY, ISSUER, AUDIENCE, eventId, eventTypeKey, eventPayload, issuedAt, expiresAt));
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Unable to sign Apple Sign-In test notification", ex);
		}
	}

	public static String signSampleDestructiveEvent(final String eventId, final String eventTypeKey) {
		return signNotification(eventId, eventTypeKey, Map.of("type", eventTypeKey, "sub", APPLE_SUBJECT, "email",
				"relay@privaterelay.appleid.com", "is_private_email", true));
	}

	public static String webhookJsonBody(final String signedPayload) {
		return "{\"payload\":\"" + signedPayload + "\"}";
	}

	public static Paciente persistApplePaciente(final PacienteRepository pacienteRepository) {
		final Paciente paciente = new Paciente();
		paciente.setName("Paciente Apple Test");
		paciente.setUserId("nutritionist-apple-test");
		paciente.setPatientAuthSub(AUTH0_USER_ID);
		paciente.setAppleSubject(APPLE_SUBJECT);
		paciente.setGender("F");
		paciente.setEmail("relay@privaterelay.appleid.com");
		paciente.setStatus(PacienteStatus.ACTIVE);
		final LocalDate dob = LocalDate.of(1992, 3, 10);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return pacienteRepository.saveAndFlush(paciente);
	}

	@TestConfiguration
	public static class TestJwksConfiguration {

		@Bean
		@Primary
		AppleSignInJwksKeySource appleSignInTestJwksKeySource() {
			return AppleSignInTestJwtSupport.keySourceFrom(RSA_KEY);
		}

	}

}
