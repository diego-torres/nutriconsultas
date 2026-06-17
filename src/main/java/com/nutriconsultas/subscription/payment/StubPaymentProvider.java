package com.nutriconsultas.subscription.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Fallback provider used when Mercado Pago credentials are not configured.
 */
@Component
@ConditionalOnMissingBean(MercadoPagoPaymentProvider.class)
public class StubPaymentProvider implements PaymentProvider {

	@Override
	public String getProviderId() {
		return "stub";
	}

	@Override
	public CheckoutSession createCheckoutSession(final Long invitationId, final PlanTier planTier,
			final BillingInterval billingInterval) {
		throw new PaymentProviderException("Payment provider is not configured");
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
		throw new PaymentProviderException("Payment provider is not configured");
	}

}
