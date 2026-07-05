package com.nutriconsultas.subscription;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nutriconsultas.ai.AiToolErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Localized 403 responses for subscription limit violations on REST endpoints (#190).
 */
@RestControllerAdvice
@Slf4j
public class SubscriptionExceptionHandler {

	private final SubscriptionErrorResponses errorResponses;

	public SubscriptionExceptionHandler(final SubscriptionErrorResponses errorResponses) {
		this.errorResponses = errorResponses;
	}

	@ExceptionHandler(SubscriptionLimitExceededException.class)
	public ResponseEntity<Map<String, Object>> handleLimitExceeded(final SubscriptionLimitExceededException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Subscription limit exceeded: messageKey={}", ex.getMessageKey());
		}
		final String message = errorResponses.resolve(ex);
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("error", message);
		body.put("message", message);
		body.put("code", ex.getMessageKey());
		body.put("errorCode", AiToolErrorCode.FORBIDDEN.name());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}

}
