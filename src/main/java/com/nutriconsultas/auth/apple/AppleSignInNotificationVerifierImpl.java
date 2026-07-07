package com.nutriconsultas.auth.apple;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppleSignInNotificationVerifierImpl implements AppleSignInNotificationVerifier {

	private final AppleSignInProperties properties;

	private final AppleSignInJwksKeySource jwksKeySource;

	private final ObjectMapper objectMapper;

	public AppleSignInNotificationVerifierImpl(final AppleSignInProperties properties,
			final AppleSignInJwksKeySource jwksKeySource, final ObjectMapper objectMapper) {
		this.properties = properties;
		this.jwksKeySource = jwksKeySource;
		this.objectMapper = objectMapper;
	}

	@Override
	public AppleSignInNotificationClaims verifyAndParse(final String signedPayload) {
		if (!StringUtils.hasText(signedPayload)) {
			throw new InvalidAppleSignInNotificationException("Apple notification payload is required");
		}
		try {
			final JWTClaimsSet claimsSet = verifySignature(signedPayload.trim());
			validateStandardClaims(claimsSet);
			return mapClaims(claimsSet);
		}
		catch (BadJOSEException ex) {
			throw new InvalidAppleSignInNotificationException("Invalid Apple notification signature", ex);
		}
		catch (ParseException | JOSEException ex) {
			throw new InvalidAppleSignInNotificationException("Unable to parse Apple notification payload", ex);
		}
	}

	private JWTClaimsSet verifySignature(final String signedPayload)
			throws BadJOSEException, ParseException, JOSEException {
		final ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		jwtProcessor
			.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwksKeySource.getKeySource()));
		return jwtProcessor.process(signedPayload, null);
	}

	private void validateStandardClaims(final JWTClaimsSet claimsSet) {
		final String issuer = claimsSet.getIssuer();
		if (!properties.getExpectedIssuer().equals(issuer)) {
			throw new InvalidAppleSignInNotificationException("Invalid Apple notification issuer");
		}
		final String audience = resolveAudience(claimsSet);
		if (!properties.getExpectedAudience().equals(audience)) {
			throw new InvalidAppleSignInNotificationException("Invalid Apple notification audience");
		}
		if (!StringUtils.hasText(claimsSet.getJWTID())) {
			throw new InvalidAppleSignInNotificationException("Apple notification event id (jti) is required");
		}
	}

	private static String resolveAudience(final JWTClaimsSet claimsSet) {
		final List<String> audiences = claimsSet.getAudience();
		if (audiences == null || audiences.isEmpty()) {
			return null;
		}
		return audiences.get(0);
	}

	private AppleSignInNotificationClaims mapClaims(final JWTClaimsSet claimsSet) throws ParseException {
		final Map<String, Object> events = readEventsClaim(claimsSet);
		final Map.Entry<String, Object> firstEvent = events.entrySet().iterator().next();
		final String rawEventType = firstEvent.getKey();
		final Map<String, Object> eventPayload = asStringObjectMap(firstEvent.getValue());
		final AppleSignInEventType eventType = AppleSignInEventType.fromAppleType(rawEventType);
		final String appleSubject = stringValue(eventPayload.get("sub"));
		final String email = stringValue(eventPayload.get("email"));
		final Boolean emailVerified = booleanValue(eventPayload.get("email_verified"));
		final Boolean isPrivateEmail = booleanValue(eventPayload.get("is_private_email"));
		final String rawClaimsJson = serializeSafeClaims(claimsSet, events);
		return new AppleSignInNotificationClaims(claimsSet.getJWTID(), claimsSet.getIssuer(),
				resolveAudience(claimsSet), appleSubject, eventType, email, emailVerified, isPrivateEmail,
				rawClaimsJson);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readEventsClaim(final JWTClaimsSet claimsSet) throws ParseException {
		final Object eventsClaim = claimsSet.getClaim("events");
		if (!(eventsClaim instanceof Map<?, ?> eventsMap) || eventsMap.isEmpty()) {
			throw new InvalidAppleSignInNotificationException("Apple notification events claim is required");
		}
		final Map<String, Object> events = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : eventsMap.entrySet()) {
			if (entry.getKey() instanceof String key) {
				events.put(key, entry.getValue());
			}
		}
		if (events.isEmpty()) {
			throw new InvalidAppleSignInNotificationException("Apple notification events claim is required");
		}
		return events;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asStringObjectMap(final Object value) {
		if (value instanceof Map<?, ?> map) {
			final Map<String, Object> normalized = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() instanceof String key) {
					normalized.put(key, entry.getValue());
				}
			}
			return normalized;
		}
		return Map.of();
	}

	private String serializeSafeClaims(final JWTClaimsSet claimsSet, final Map<String, Object> events) {
		final Map<String, Object> safeClaims = new LinkedHashMap<>();
		safeClaims.put("iss", claimsSet.getIssuer());
		safeClaims.put("aud", resolveAudience(claimsSet));
		safeClaims.put("jti", claimsSet.getJWTID());
		safeClaims.put("events", events);
		try {
			return objectMapper.writeValueAsString(safeClaims);
		}
		catch (JsonProcessingException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Unable to serialize Apple notification claims for audit storage");
			}
			return null;
		}
	}

	private static String stringValue(final Object value) {
		if (value instanceof String text && StringUtils.hasText(text)) {
			return text.trim();
		}
		return null;
	}

	private static Boolean booleanValue(final Object value) {
		if (value instanceof Boolean bool) {
			return bool;
		}
		return null;
	}

}
