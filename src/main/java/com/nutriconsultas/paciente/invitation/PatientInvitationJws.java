package com.nutriconsultas.paciente.invitation;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimal HS256 JWS for optional Auth0 Post-Login offline verification (#133 / #140).
 */
final class PatientInvitationJws {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private PatientInvitationJws() {
	}

	static String sign(final String secret, final long pacienteId, final String tokenHash, final Instant expiresAt) {
		final String header = BASE64_URL
			.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		final String payloadJson = buildPayload(pacienteId, tokenHash, expiresAt);
		final String payload = BASE64_URL.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		final String signingInput = header + "." + payload;
		final String signature = BASE64_URL.encodeToString(hmacSha256(secret, signingInput));
		return signingInput + "." + signature;
	}

	static Optional<Long> verify(final String secret, final String compactJws) {
		if (compactJws == null || compactJws.isBlank()) {
			return Optional.empty();
		}
		final String[] parts = compactJws.split("\\.");
		if (parts.length != 3) {
			return Optional.empty();
		}
		if (!isHs256Header(parts[0])) {
			return Optional.empty();
		}
		final String signingInput = parts[0] + "." + parts[1];
		final byte[] expected = hmacSha256(secret, signingInput);
		final byte[] actual;
		try {
			actual = BASE64_URL_DECODER.decode(parts[2]);
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		if (!MessageDigest.isEqual(expected, actual)) {
			return Optional.empty();
		}
		try {
			final JsonNode payload = MAPPER
				.readTree(new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
			final long exp = payload.path("exp").asLong(0L);
			if (exp <= Instant.now().getEpochSecond()) {
				return Optional.empty();
			}
			final long patientId = payload.path("patientId").asLong(-1L);
			if (patientId < 1L) {
				return Optional.empty();
			}
			return Optional.of(patientId);
		}
		catch (IllegalArgumentException | JsonProcessingException ex) {
			return Optional.empty();
		}
	}

	private static String buildPayload(final long pacienteId, final String tokenHash, final Instant expiresAt) {
		try {
			return MAPPER.writeValueAsString(new Payload(pacienteId, tokenHash, expiresAt.getEpochSecond()));
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize JWS payload", ex);
		}
	}

	private static boolean isHs256Header(final String encodedHeader) {
		try {
			final JsonNode header = MAPPER
				.readTree(new String(BASE64_URL_DECODER.decode(encodedHeader), StandardCharsets.UTF_8));
			return "HS256".equals(header.path("alg").asText());
		}
		catch (IllegalArgumentException | JsonProcessingException ex) {
			return false;
		}
	}

	private static byte[] hmacSha256(final String secret, final String signingInput) {
		try {
			final Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("HmacSHA256 not available", ex);
		}
	}

	private record Payload(long patientId, String tokenHash, long exp) {

	}

}
