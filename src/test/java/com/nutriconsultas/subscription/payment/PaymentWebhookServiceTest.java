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
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.invitation.SubscriptionProvisioningService;

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

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private SubscriptionProvisioningService provisioningService;

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
		final NutritionistInvitation invitation = redeemedInvitation(subscription);
		when(paymentProvider.getProviderId()).thenReturn(PaymentProperties.PROVIDER_MERCADOPAGO);
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(true);
		when(paymentProvider.parseWebhook(PAYLOAD, headers)).thenReturn(successEvent());
		when(webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_MERCADOPAGO, "evt-1"))
			.thenReturn(Optional.empty());
		when(subscriptionRepository.findByExternalSubscriptionId("mp-sub-1")).thenReturn(Optional.of(subscription));
		when(invitationRepository.findBySubscriptionId(subscription.getId())).thenReturn(Optional.of(invitation));

		final PaymentWebhookResult result = paymentWebhookService.handleWebhook(PAYLOAD, headers);

		assertThat(result.outcome()).isEqualTo(PaymentWebhookOutcome.PROCESSED);
		assertThat(result.subscriptionId()).isEqualTo(subscription.getId());
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(subscription.getPeriodStart()).isNotNull();
		assertThat(subscription.getPeriodEnd()).isNotNull();
		verify(subscriptionRepository).save(subscription);
		verify(provisioningService).activatePaidAccess(invitation, subscription);
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

	@Test
	void handleWebhookPersistsProviderSubscriptionIdAfterCheckout() {
		final Subscription subscription = pendingSubscription("cs_test_1");
		when(paymentProvider.getProviderId()).thenReturn(PaymentProperties.PROVIDER_STRIPE);
		when(paymentProvider.verifyWebhookSignature(PAYLOAD, headers)).thenReturn(true);
		when(paymentProvider.parseWebhook(PAYLOAD, headers))
			.thenReturn(new ParsedPaymentWebhookEvent("evt-2", "checkout.session.completed", "cs_test_1", "cus_test_1",
					"sub_test_1", PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE));
		when(webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_STRIPE, "evt-2"))
			.thenReturn(Optional.empty());
		when(subscriptionRepository.findByExternalSubscriptionId("cs_test_1")).thenReturn(Optional.of(subscription));
		when(invitationRepository.findBySubscriptionId(subscription.getId())).thenReturn(Optional.empty());

		paymentWebhookService.handleWebhook(PAYLOAD, headers);

		assertThat(subscription.getExternalSubscriptionId()).isEqualTo("sub_test_1");
		assertThat(subscription.getExternalCustomerId()).isEqualTo("cus_test_1");
	}

	private static ParsedPaymentWebhookEvent successEvent() {
		return new ParsedPaymentWebhookEvent("evt-1", "subscription_preapproval", "mp-sub-1", "cust-1", null,
				PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE);
	}

	private static Subscription pendingSubscription(final String externalSubscriptionId) {
		final Subscription subscription = new Subscription();
		subscription.setId(42L);
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		subscription.setExternalSubscriptionId(externalSubscriptionId);
		return subscription;
	}

	private static NutritionistInvitation redeemedInvitation(final Subscription subscription) {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(7L);
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setRedeemedByUserId("auth0|invitee");
		invitation.setSubscription(subscription);
		return invitation;
	}

}
