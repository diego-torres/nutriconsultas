package com.nutriconsultas.mobile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.dto.ApiResponse;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;

/**
 * Localized error responses for {@code /rest/mobile/**} (#111, #113).
 */
@RestControllerAdvice(basePackages = "com.nutriconsultas.mobile")
@Slf4j
public class MobileApiExceptionHandler {

	private static final String RATE_LIMIT_RETRY_AFTER_SECONDS = "60";

	private final MobileApiErrorResponses errorResponses;

	public MobileApiExceptionHandler(final MobileApiErrorResponses errorResponses) {
		this.errorResponses = errorResponses;
	}

	@ExceptionHandler(PatientNotLinkedException.class)
	public ResponseEntity<ApiResponse<Void>> handlePatientNotLinked(final PatientNotLinkedException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API patient not linked");
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_NOT_LINKED));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatus(final ResponseStatusException ex) {
		if (!errorResponses.isNotFound(ex)) {
			throw ex;
		}
		if (log.isDebugEnabled()) {
			log.debug("Mobile API resource not found");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_RESOURCE_NOT_FOUND));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(final MethodArgumentNotValidException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API validation failed");
		}
		final String messageKey = errorResponses.validationMessageKey(ex);
		return ResponseEntity.badRequest().body(errorResponses.error(messageKey));
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(final RequestNotPermitted ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API rate limit exceeded");
		}
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, RATE_LIMIT_RETRY_AFTER_SECONDS)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_RATE_LIMIT_EXCEEDED));
	}

}
