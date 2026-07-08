package com.nutriconsultas.auth.apple;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/webhooks/apple")
@Slf4j
public class AppleSignInWebhookController {

	private final AppleSignInProperties properties;

	private final AppleSignInNotificationService notificationService;

	private final AppleSignInWebhookObservability webhookObservability;

	public AppleSignInWebhookController(final AppleSignInProperties properties,
			final AppleSignInNotificationService notificationService,
			final AppleSignInWebhookObservability webhookObservability) {
		this.properties = properties;
		this.notificationService = notificationService;
		this.webhookObservability = webhookObservability;
	}

	@PostMapping("/sign-in")
	public ResponseEntity<Void> handleSignInNotification(@RequestBody final AppleSignInWebhookRequest request) {
		if (!properties.isWebhookEnabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		if (request == null || !StringUtils.hasText(request.payload())) {
			return ResponseEntity.badRequest().build();
		}
		webhookObservability.recordWebhookReceived();
		final AppleSignInWebhookOutcome outcome = notificationService.handleNotification(request.payload().trim());
		if (outcome == AppleSignInWebhookOutcome.DUPLICATE) {
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.ok().build();
	}

}
