package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AppleSignInIntegrationTestSupport.TestJwksConfiguration.class)
@TestPropertySource(properties = { "nutriconsultas.apple.signin.webhook.enabled=true",
		"nutriconsultas.apple.signin.expected-audience=com.minutriporcion.app" })
class AppleSignInWebhookIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppleSignInNotificationRepository notificationRepository;

	@BeforeEach
	void cleanNotifications() {
		notificationRepository.deleteAll();
	}

	@Test
	void webhookIsReachableWithoutAuthentication() throws Exception {
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-public-access",
				"consent-revoked");

		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content(AppleSignInIntegrationTestSupport.webhookJsonBody(signedPayload)))
			.andExpect(status().isOk());
	}

	@Test
	void returnsBadRequestWhenPayloadMissing() throws Exception {
		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void returnsBadRequestWhenPayloadMalformed() throws Exception {
		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"payload\":\"not-a-valid-jwt\"}"))
			.andExpect(status().isBadRequest());

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void returnsBadRequestWhenSignatureInvalid() throws Exception {
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-bad-signature",
				"consent-revoked");
		final String tamperedPayload = signedPayload.substring(0, signedPayload.length() - 4) + "xxxx";

		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content(AppleSignInIntegrationTestSupport.webhookJsonBody(tamperedPayload)))
			.andExpect(status().isBadRequest());

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void acceptsValidPayloadAndPersistsNotification() throws Exception {
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-valid-1",
				"consent-revoked");

		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content(AppleSignInIntegrationTestSupport.webhookJsonBody(signedPayload)))
			.andExpect(status().isOk());

		assertThat(notificationRepository.findByAppleEventId("evt-valid-1")).isPresent()
			.get()
			.satisfies(notification -> {
				assertThat(notification.getEventType()).isEqualTo(AppleSignInEventType.CONSENT_REVOKED);
				assertThat(notification.getAppleSubject()).isEqualTo(AppleSignInIntegrationTestSupport.APPLE_SUBJECT);
				assertThat(notification.getProcessingStatus())
					.isEqualTo(AppleSignInNotificationProcessingStatus.PROCESSED);
			});
	}

	@Test
	void returnsOkForDuplicateWithoutSecondPersistedRow() throws Exception {
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-dup-1",
				"email-enabled");

		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content(AppleSignInIntegrationTestSupport.webhookJsonBody(signedPayload)))
			.andExpect(status().isOk());
		mockMvc
			.perform(post(AppleSignInIntegrationTestSupport.WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content(AppleSignInIntegrationTestSupport.webhookJsonBody(signedPayload)))
			.andExpect(status().isOk());

		assertThat(notificationRepository.findByAppleEventId("evt-dup-1")).isPresent();
		assertThat(notificationRepository.count()).isEqualTo(1);
	}

}
