package com.nutriconsultas.subscription.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;

/**
 * Fallback provider used when Mercado Pago credentials are not configured. Can simulate
 * checkout locally when {@link PaymentProperties#isStubSimulateCheckout()} is enabled.
 */
@Component
@ConditionalOnMissingBean(MercadoPagoPaymentProvider.class)
public class StubPaymentProvider implements PaymentProvider {

	private static final String STUB_EXTERNAL_ID_PREFIX = "stub-sub-";

	private final PaymentProperties paymentProperties;

	private final NutritionistInvitationRepository invitationRepository;

	public StubPaymentProvider(final PaymentProperties paymentProperties,
			final NutritionistInvitationRepository invitationRepository) {
		this.paymentProperties = paymentProperties;
		this.invitationRepository = invitationRepository;
	}

	@Override
	public String getProviderId() {
		return "stub";
	}

	@Override
	public CheckoutSession createCheckoutSession(final Long invitationId, final PlanTier planTier,
			final BillingInterval billingInterval) {
		if (!paymentProperties.isStubSimulateCheckout()) {
			throw new PaymentProviderException("Payment provider is not configured");
		}
		if (invitationId == null) {
			throw new PaymentProviderException("invitationId is required");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new PaymentProviderException("Invitation not found"));
		final Long subscriptionId = invitation.getSubscription() != null ? invitation.getSubscription().getId() : null;
		final String externalSubscriptionId = STUB_EXTERNAL_ID_PREFIX + invitationId;
		final String checkoutUrl = "/invitation/nutritionist/dev-checkout?invitationId=" + invitationId;
		return new CheckoutSession(subscriptionId, checkoutUrl, externalSubscriptionId, "stub-cust-" + invitationId);
	}

	@Override
	public boolean verifyWebhookSignature(final String payload, final PaymentWebhookHeaders headers) {
		return false;
	}

	@Override
	public ParsedPaymentWebhookEvent parseWebhook(final String payload, final PaymentWebhookHeaders headers) {
		throw new PaymentProviderException("Payment provider is not configured");
	}

	@Override
	public void cancelSubscription(final String externalSubscriptionId) {
		if (paymentProperties.isStubSimulateCheckout() && isStubExternalId(externalSubscriptionId)) {
			return;
		}
		throw new PaymentProviderException("Payment provider is not configured");
	}

	private static boolean isStubExternalId(final String externalSubscriptionId) {
		return externalSubscriptionId != null && externalSubscriptionId.startsWith(STUB_EXTERNAL_ID_PREFIX);
	}

}
