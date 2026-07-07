package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "nutriconsultas.apple.signin.webhook.enabled=true",
		"nutriconsultas.apple.signin.expected-audience=com.minutriporcion.app" })
class AppleSignInWebhookSecurityTest {

	private static final String WEBHOOK_URL = "/rest/webhooks/apple/sign-in";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AppleSignInNotificationService notificationService;

	@Test
	void webhookIsReachableWithoutAuthentication() throws Exception {
		when(notificationService.handleNotification(any())).thenReturn(AppleSignInWebhookOutcome.PROCESSED);

		final int status = mockMvc
			.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON).content("{\"payload\":\"signed-jwt\"}"))
			.andReturn()
			.getResponse()
			.getStatus();

		assertThat(status).isEqualTo(200);
	}

}
