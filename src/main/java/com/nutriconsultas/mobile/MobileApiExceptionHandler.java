package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0PatientAuthenticationException;
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

	@ExceptionHandler(PatientInvitationRevokeNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationRevokeNotFound(
			final PatientInvitationRevokeNotFoundException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation revoke not found");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_UNAVAILABLE));
	}

	@ExceptionHandler(PatientInvitationRevokeNotAllowedException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvitationRevokeNotAllowed(
			final PatientInvitationRevokeNotAllowedException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API invitation revoke not allowed");
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_INVITATION_REVOKE_NOT_ALLOWED));
	}

	@ExceptionHandler(PatientAuthBrokerNotConfiguredException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthBrokerNotConfigured(
			final PatientAuthBrokerNotConfiguredException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API patient auth broker not configured");
		}
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(errorResponses.error(MobileApiErrorResponses.KEY_AUTH_BROKER_NOT_CONFIGURED));
	}

	@ExceptionHandler(PatientAuthEmailMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthEmailMismatch(final PatientAuthEmailMismatchException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API signup email mismatch");
		}
		return ResponseEntity.badRequest().body(errorResponses.error(MobileApiErrorResponses.KEY_AUTH_EMAIL_MISMATCH));
	}

	@ExceptionHandler(Auth0PatientAuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuth0PatientAuthentication(
			final Auth0PatientAuthenticationException ex) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile API Auth0 patient authentication failed code={}", ex.getErrorCode());
		}
		final String messageKey = switch (ex.getErrorCode()) {
			case Auth0PatientAuthenticationException.CODE_INVALID_CREDENTIALS ->
				MobileApiErrorResponses.KEY_AUTH_INVALID_CREDENTIALS;
			case Auth0PatientAuthenticationException.CODE_EMAIL_IN_USE -> MobileApiErrorResponses.KEY_AUTH_EMAIL_IN_USE;
			case Auth0PatientAuthenticationException.CODE_WEAK_PASSWORD ->
				MobileApiErrorResponses.KEY_AUTH_WEAK_PASSWORD;
			case Auth0PatientAuthenticationException.CODE_INVITATION_REQUIRED ->
				MobileApiErrorResponses.KEY_INVITATION_UNAVAILABLE;
			default -> MobileApiErrorResponses.KEY_AUTH_FAILED;
		};
		return ResponseEntity.status(ex.getStatusCode()).body(errorResponses.error(messageKey));
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
