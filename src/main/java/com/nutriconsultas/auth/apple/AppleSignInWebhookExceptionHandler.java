package com.nutriconsultas.auth.apple;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.nutriconsultas.auth.apple")
public class AppleSignInWebhookExceptionHandler {

	private final AppleSignInWebhookObservability webhookObservability;

	public AppleSignInWebhookExceptionHandler(final AppleSignInWebhookObservability webhookObservability) {
		this.webhookObservability = webhookObservability;
	}

	@ExceptionHandler(InvalidAppleSignInNotificationException.class)
	public ResponseEntity<Void> handleInvalidNotification(final InvalidAppleSignInNotificationException ex) {
		webhookObservability.recordVerificationFailure(ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

}
