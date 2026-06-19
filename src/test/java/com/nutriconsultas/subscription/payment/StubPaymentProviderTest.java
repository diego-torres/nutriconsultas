package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;

@ExtendWith(MockitoExtension.class)
class StubPaymentProviderTest {

	@InjectMocks
	private StubPaymentProvider provider;

	@Mock
	private PaymentProperties paymentProperties;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Test
	void createCheckoutSession_whenSimulateEnabled_returnsDevCheckoutUrl() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(9L);
		final Subscription subscription = new Subscription();
		subscription.setId(3L);
		invitation.setSubscription(subscription);
		when(paymentProperties.isStubSimulateCheckout()).thenReturn(true);
		when(invitationRepository.findById(9L)).thenReturn(Optional.of(invitation));

		final CheckoutSession session = provider.createCheckoutSession(9L, PlanTier.BASICO, BillingInterval.MONTHLY);

		assertThat(session.checkoutUrl()).isEqualTo("/invitation/nutritionist/dev-checkout?invitationId=9");
		assertThat(session.externalSubscriptionId()).isEqualTo("stub-sub-9");
	}

	@Test
	void createCheckoutSession_whenSimulateDisabled_throws() {
		when(paymentProperties.isStubSimulateCheckout()).thenReturn(false);

		assertThatThrownBy(() -> provider.createCheckoutSession(9L, PlanTier.BASICO, BillingInterval.MONTHLY))
			.isInstanceOf(PaymentProviderException.class)
			.hasMessageContaining("not configured");
	}

	@Test
	void cancelSubscription_whenStubExternalId_noops() {
		when(paymentProperties.isStubSimulateCheckout()).thenReturn(true);

		provider.cancelSubscription("stub-sub-9");
	}

}
