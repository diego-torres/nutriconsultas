package com.nutriconsultas.subscription.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/subscription/payment")
@Slf4j
public class PaymentWebhookController {

	private final PaymentWebhookService paymentWebhookService;

	public PaymentWebhookController(final PaymentWebhookService paymentWebhookService) {
		this.paymentWebhookService = paymentWebhookService;
	}

	@PostMapping("/webhook")
	public ResponseEntity<Void> handleWebhook(@RequestBody final String payload,
			@RequestHeader(value = "x-signature", required = false) final String signature,
			@RequestHeader(value = "x-request-id", required = false) final String requestId,
			@RequestParam(value = "data.id", required = false) final String dataId) {
		final PaymentWebhookHeaders headers = new PaymentWebhookHeaders(signature, requestId, dataId);
		final PaymentWebhookResult result = paymentWebhookService.handleWebhook(payload, headers);
		if (result.outcome() == PaymentWebhookOutcome.DUPLICATE) {
			return ResponseEntity.ok().build();
		}
		if (result.outcome() == PaymentWebhookOutcome.IGNORED) {
			return ResponseEntity.accepted().build();
		}
		return ResponseEntity.status(HttpStatus.OK).build();
	}

}
