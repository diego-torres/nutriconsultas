package com.nutriconsultas.subscription.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.nutriconsultas.subscription.payment")
public class PaymentWebhookExceptionHandler {

	@ExceptionHandler(InvalidPaymentWebhookException.class)
	public ResponseEntity<Void> handleInvalidWebhook(final InvalidPaymentWebhookException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

}
