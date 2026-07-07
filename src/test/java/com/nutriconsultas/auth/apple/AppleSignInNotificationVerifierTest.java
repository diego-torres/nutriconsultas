package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

@ExtendWith(MockitoExtension.class)
class AppleSignInNotificationVerifierTest {

	private static final String ISSUER = "https://appleid.apple.com";

	private static final String AUDIENCE = "com.minutriporcion.app";

	private RSAKey rsaKey;

	private AppleSignInNotificationVerifier verifier;

	@BeforeEach
	void setUp() throws JOSEException {
		rsaKey = AppleSignInTestJwtSupport.generateRsaKey();
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.setExpectedAudience(AUDIENCE);
		verifier = new AppleSignInNotificationVerifierImpl(properties, AppleSignInTestJwtSupport.keySourceFrom(rsaKey),
				new ObjectMapper());
	}

	@Test
	void verifyAndParseAcceptsValidPayload() throws Exception {
		final String signedPayload = signSampleNotification("evt-valid", "email-disabled");

		final AppleSignInNotificationClaims claims = verifier.verifyAndParse(signedPayload);

		assertThat(claims.eventId()).isEqualTo("evt-valid");
		assertThat(claims.eventType()).isEqualTo(AppleSignInEventType.EMAIL_DISABLED);
		assertThat(claims.appleSubject()).isEqualTo("001234.abc");
		assertThat(claims.email()).isEqualTo("relay@privaterelay.appleid.com");
		assertThat(claims.isPrivateEmail()).isTrue();
	}

	@Test
	void verifyAndParseRejectsInvalidSignature() throws Exception {
		final String signedPayload = signSampleNotification("evt-bad-signature", "email-disabled");
		final String tamperedPayload = signedPayload.substring(0, signedPayload.length() - 4) + "xxxx";

		assertThatThrownBy(() -> verifier.verifyAndParse(tamperedPayload))
			.isInstanceOf(InvalidAppleSignInNotificationException.class);
	}

	@Test
	void verifyAndParseRejectsWrongIssuer() throws Exception {
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.setExpectedIssuer("https://evil.example");
		properties.setExpectedAudience(AUDIENCE);
		final AppleSignInNotificationVerifier strictVerifier = new AppleSignInNotificationVerifierImpl(properties,
				AppleSignInTestJwtSupport.keySourceFrom(rsaKey), new ObjectMapper());
		final String signedPayload = signSampleNotification("evt-issuer", "email-disabled");

		assertThatThrownBy(() -> strictVerifier.verifyAndParse(signedPayload))
			.isInstanceOf(InvalidAppleSignInNotificationException.class)
			.hasMessageContaining("issuer");
	}

	@Test
	void verifyAndParseRejectsWrongAudience() throws Exception {
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.setExpectedAudience("com.other.app");
		final AppleSignInNotificationVerifier strictVerifier = new AppleSignInNotificationVerifierImpl(properties,
				AppleSignInTestJwtSupport.keySourceFrom(rsaKey), new ObjectMapper());
		final String signedPayload = signSampleNotification("evt-audience", "email-disabled");

		assertThatThrownBy(() -> strictVerifier.verifyAndParse(signedPayload))
			.isInstanceOf(InvalidAppleSignInNotificationException.class)
			.hasMessageContaining("audience");
	}

	@Test
	void verifyAndParseRejectsExpiredPayload() throws Exception {
		final Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS);
		final Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
		final String signedPayload = AppleSignInTestJwtSupport
			.signNotification(new AppleSignInTestJwtSupport.SignNotificationRequest(rsaKey, ISSUER, AUDIENCE,
					"evt-expired", "email-disabled", sampleEventPayload(), issuedAt, expiresAt));

		assertThatThrownBy(() -> verifier.verifyAndParse(signedPayload))
			.isInstanceOf(InvalidAppleSignInNotificationException.class);
	}

	private String signSampleNotification(final String eventId, final String eventKey) throws JOSEException {
		final Instant issuedAt = Instant.now();
		final Instant expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);
		return AppleSignInTestJwtSupport.signNotification(new AppleSignInTestJwtSupport.SignNotificationRequest(rsaKey,
				ISSUER, AUDIENCE, eventId, eventKey, sampleEventPayload(), issuedAt, expiresAt));
	}

	private static Map<String, Object> sampleEventPayload() {
		return Map.of("type", "email-disabled", "sub", "001234.abc", "email", "relay@privaterelay.appleid.com",
				"is_private_email", true);
	}

}
