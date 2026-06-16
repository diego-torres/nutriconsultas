package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

	private static final String PAYLOAD = "{\"type\":\"subscription_preapproval\",\"action\":\"updated\"}";

	@Mock
	private PaymentProvider paymentProvider;

	@Mock
	private PaymentWebhookEventRepository webhookEventRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@InjectMocks
	private PaymentWebhookService paymentWebhookService;

	private PaymentWebhookHeaders headers;

	@BeforeEach
	void setUp() {
		headers = new PaymentWebhookHeaders("ts=1,v1=abc", "req-1", "pre-1");
	}

	@Test
	void handleWebhookRejectsInvalidSignature() {
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(false);

		assertThatThrownBy(() -> paymentWebhookService.handleWebhook(PAYLOAD, headers))
			.isInstanceOf(InvalidPaymentWebhookException.class);
	}

	@Test
	void handleWebhookReturnsDuplicateWhenEventAlreadyProcessed() {
		when(paymentProvider.getProviderId()).thenReturn(PaymentProperties.PROVIDER_MERCADOPAGO);
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(true);
		when(paymentProvider.parseWebhook(PAYLOAD, headers)).thenReturn(successEvent());
		when(webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_MERCADOPAGO, "evt-1"))
			.thenReturn(Optional.of(new PaymentWebhookEvent()));

		final PaymentWebhookResult result = paymentWebhookService.handleWebhook(PAYLOAD, headers);

		assertThat(result.outcome()).isEqualTo(PaymentWebhookOutcome.DUPLICATE);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void handleWebhookActivatesSubscriptionOnPaymentSucceeded() {
		final Subscription subscription = pendingSubscription("mp-sub-1");
		when(paymentProvider.getProviderId()).thenReturn(PaymentProperties.PROVIDER_MERCADOPAGO);
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(true);
		when(paymentProvider.parseWebhook(PAYLOAD, headers)).thenReturn(successEvent());
		when(webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_MERCADOPAGO, "evt-1"))
			.thenReturn(Optional.empty());
		when(subscriptionRepository.findByExternalSubscriptionId("mp-sub-1")).thenReturn(Optional.of(subscription));

		final PaymentWebhookResult result = paymentWebhookService.handleWebhook(PAYLOAD, headers);

		assertThat(result.outcome()).isEqualTo(PaymentWebhookOutcome.PROCESSED);
		assertThat(result.subscriptionId()).isEqualTo(subscription.getId());
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(subscription.getPeriodStart()).isNotNull();
		assertThat(subscription.getPeriodEnd()).isNotNull();
		verify(subscriptionRepository).save(subscription);
		verify(auditEventRepository).save(any());
		final ArgumentCaptor<PaymentWebhookEvent> eventCaptor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
		verify(webhookEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventId()).isEqualTo("evt-1");
	}

	@Test
	void handleWebhookIgnoresUnknownResource() {
		when(paymentProvider.getProviderId()).thenReturn(PaymentProperties.PROVIDER_MERCADOPAGO);
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(true);
		when(paymentProvider.parseWebhook(PAYLOAD, headers)).thenReturn(successEvent());
		when(webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_MERCADOPAGO, "evt-1"))
			.thenReturn(Optional.empty());
		when(subscriptionRepository.findByExternalSubscriptionId("mp-sub-1")).thenReturn(Optional.empty());

		final PaymentWebhookResult result = paymentWebhookService.handleWebhook(PAYLOAD, headers);

		assertThat(result.outcome()).isEqualTo(PaymentWebhookOutcome.IGNORED);
		verify(webhookEventRepository, never()).save(any());
	}

	private static ParsedPaymentWebhookEvent successEvent() {
		return new ParsedPaymentWebhookEvent("evt-1", "subscription_preapproval", "mp-sub-1", "cust-1",
				PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE);
	}

	private static Subscription pendingSubscription(final String externalSubscriptionId) {
		final Subscription subscription = new Subscription();
		subscription.setId(42L);
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		subscription.setExternalSubscriptionId(externalSubscriptionId);
		return subscription;
	}

}
