package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

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

	private final SubscriptionErrorResponses subscriptionErrorResponses;

	public MobileApiExceptionHandler(final MobileApiErrorResponses errorResponses,
			final SubscriptionErrorResponses subscriptionErrorResponses) {
		this.errorResponses = errorResponses;
		this.subscriptionErrorResponses = subscriptionErrorResponses;
	}

	@ExceptionHandler(PatientNotLinkedException.class)
	public ResponseEntity<ApiResponse<Void>> handlePatientNotLinked(final PatientNotLinkedException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API patient not linked");
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_NOT_LINKED));
	}

	@ExceptionHandler(PatientOnboardingRequiredException.class)
	public ResponseEntity<ApiResponse<Void>> handlePatientOnboardingRequired(
			final PatientOnboardingRequiredException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API onboarding required");
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_ONBOARDING_REQUIRED));
	}

	@ExceptionHandler(PatientOnboardingInvalidAvatarException.class)
	public ResponseEntity<ApiResponse<Void>> handlePatientOnboardingInvalidAvatar(
			final PatientOnboardingInvalidAvatarException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API onboarding invalid avatar");
		}
		return ResponseEntity.badRequest()
			.body(errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_ONBOARDING_INVALID_AVATAR));
	}

	@ExceptionHandler(SubscriptionLimitExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleSubscriptionLimit(final SubscriptionLimitExceededException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API subscription limit exceeded: messageKey={}", ex.getMessageKey());
		}
		final String message = subscriptionErrorResponses.resolve(ex);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(null, message, Instant.now()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatus(final ResponseStatusException ex) {
		if (errorResponses.isNotFound(ex)) {
			if (log.isDebugEnabled()) {
				log.debug("Mobile API resource not found");
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(errorResponses.error(MobileApiErrorResponses.KEY_RESOURCE_NOT_FOUND));
		}
		if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
			if (log.isDebugEnabled()) {
				log.debug("Mobile API conflict");
			}
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_ASSIGNED_ID_TAKEN));
		}
		throw ex;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(final MethodArgumentNotValidException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API validation failed");
		}
		final String messageKey = errorResponses.validationMessageKey(ex);
		return ResponseEntity.badRequest().body(errorResponses.error(messageKey));
	}

	@ExceptionHandler(PatientInvitationInvalidTokenException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationInvalidToken(
			final PatientInvitationInvalidTokenException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation token malformed");
		}
		return ResponseEntity.badRequest().body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_INVALID));
	}

	@ExceptionHandler(PatientInvitationUnavailableException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationUnavailable(
			final PatientInvitationUnavailableException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation unavailable");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_UNAVAILABLE));
	}

	@ExceptionHandler(PatientInvitationRedeemConflictException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationRedeemConflict(
			final PatientInvitationRedeemConflictException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation redeem conflict");
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_REDEEM_CONFLICT));
	}

	@ExceptionHandler(PatientInvitationPatientStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationPatientStatus(
			final PatientInvitationPatientStatusException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation patient status guard");
		}
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_PATIENT_STATUS));
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
