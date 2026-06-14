package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

	private final MessageSource messageSource;

	public MobileApiExceptionHandler(final MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(final RequestNotPermitted ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API rate limit exceeded");
		}
		final String message = messageSource.getMessage("error.rate.limit.exceeded", null,
				LocaleContextHolder.getLocale());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, RATE_LIMIT_RETRY_AFTER_SECONDS)
			.body(new ApiResponse<>(null, message, Instant.now()));
	}

}
