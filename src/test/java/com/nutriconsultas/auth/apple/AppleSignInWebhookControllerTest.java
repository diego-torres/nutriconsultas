package com.nutriconsultas.auth.apple;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AppleSignInWebhookControllerTest {

	private static final String WEBHOOK_URL = "/rest/webhooks/apple/sign-in";

	@Mock
	private AppleSignInProperties properties;

	@Mock
	private AppleSignInNotificationService notificationService;

	@Mock
	private AppleSignInWebhookObservability webhookObservability;

	@InjectMocks
	private AppleSignInWebhookController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new AppleSignInWebhookExceptionHandler(webhookObservability))
			.build();
	}

	@Test
	void returnsServiceUnavailableWhenWebhookDisabled() throws Exception {
		when(properties.isWebhookEnabled()).thenReturn(false);

		mockMvc
			.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"payload\":\"eyJ.test.signature\"}"))
			.andExpect(status().isServiceUnavailable());

		verify(notificationService, never()).handleNotification(any());
	}

	@Test
	void returnsBadRequestWhenPayloadMissing() throws Exception {
		when(properties.isWebhookEnabled()).thenReturn(true);

		mockMvc.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void returnsOkWhenNotificationProcessed() throws Exception {
		when(properties.isWebhookEnabled()).thenReturn(true);
		when(notificationService.handleNotification("signed-jwt")).thenReturn(AppleSignInWebhookOutcome.PROCESSED);

		mockMvc
			.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON).content("{\"payload\":\"signed-jwt\"}"))
			.andExpect(status().isOk());

		verify(webhookObservability).recordWebhookReceived();
	}

	@Test
	void returnsBadRequestWhenVerificationFails() throws Exception {
		when(properties.isWebhookEnabled()).thenReturn(true);
		when(notificationService.handleNotification("bad-jwt"))
			.thenThrow(new InvalidAppleSignInNotificationException("Invalid Apple notification signature"));

		mockMvc.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON).content("{\"payload\":\"bad-jwt\"}"))
			.andExpect(status().isBadRequest());

		verify(webhookObservability).recordWebhookReceived();
		verify(webhookObservability).recordVerificationFailure("Invalid Apple notification signature");
	}

}
