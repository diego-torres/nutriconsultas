package com.nutriconsultas.subscription.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class PaymentWebhookControllerTest {

	private static final String WEBHOOK_URL = "/rest/subscription/payment/webhook";

	@Mock
	private PaymentWebhookService paymentWebhookService;

	@InjectMocks
	private PaymentWebhookController paymentWebhookController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(paymentWebhookController)
			.setControllerAdvice(new PaymentWebhookExceptionHandler())
			.build();
	}

	@Test
	void webhookReturnsOkWhenProcessed() throws Exception {
		when(paymentWebhookService.handleWebhook(any(), any())).thenReturn(PaymentWebhookResult.processed(42L));

		mockMvc
			.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"subscription_preapproval\"}")
				.header("x-signature", "ts=1,v1=abc")
				.header("x-request-id", "req-1")
				.param("data.id", "123"))
			.andExpect(status().isOk());
	}

	@Test
	void webhookReturnsBadRequestWhenSignatureInvalid() throws Exception {
		when(paymentWebhookService.handleWebhook(any(), any()))
			.thenThrow(new InvalidPaymentWebhookException("Invalid payment webhook signature"));

		mockMvc
			.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"subscription_preapproval\"}")
				.header("x-signature", "invalid")
				.header("x-request-id", "req-1")
				.param("data.id", "123"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void webhookAcceptsUnauthenticatedRequests() throws Exception {
		when(paymentWebhookService.handleWebhook(eq("{\"type\":\"ignored\"}"), any()))
			.thenReturn(PaymentWebhookResult.ignored());

		mockMvc.perform(post(WEBHOOK_URL).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"ignored\"}"))
			.andExpect(status().isAccepted());
	}

}
