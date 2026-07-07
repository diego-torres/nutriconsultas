package com.nutriconsultas.auth.apple;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public final class AppleSignInTestJwtSupport {

	private AppleSignInTestJwtSupport() {
	}

	public static RSAKey generateRsaKey() throws JOSEException {
		return new RSAKeyGenerator(2048).keyID("apple-signin-test-key").generate();
	}

	public static AppleSignInJwksKeySource keySourceFrom(final RSAKey rsaKey) {
		final ImmutableJWKSet<SecurityContext> jwkSet = new ImmutableJWKSet<>(new JWKSet(rsaKey.toPublicJWK()));
		return () -> jwkSet;
	}

	public static String signNotification(final SignNotificationRequest request) throws JOSEException {
		final Map<String, Object> events = new LinkedHashMap<>();
		events.put(request.eventKey(), request.eventPayload());
		final JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(request.issuer())
			.audience(List.of(request.audience()))
			.jwtID(request.eventId())
			.issueTime(Date.from(request.issuedAt()))
			.expirationTime(Date.from(request.expiresAt()))
			.claim("events", events)
			.build();
		final SignedJWT signedJwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(request.rsaKey().getKeyID()).build(), claims);
		signedJwt.sign(new RSASSASigner(request.rsaKey()));
		return signedJwt.serialize();
	}

	public record SignNotificationRequest(RSAKey rsaKey, String issuer, String audience, String eventId,
			String eventKey, Map<String, Object> eventPayload, Instant issuedAt, Instant expiresAt) {
	}

}
