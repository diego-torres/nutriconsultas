package com.nutriconsultas.subscription.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Orchestrates checkout session creation via the configured payment provider.
 */
@Service
public class PaymentCheckoutService {

	private final PaymentProvider paymentProvider;

	public PaymentCheckoutService(final PaymentProvider paymentProvider) {
		this.paymentProvider = paymentProvider;
	}

	@Transactional
	public CheckoutSession createCheckoutSession(final Long invitationId, final PlanTier planTier,
			final BillingInterval billingInterval) {
		return paymentProvider.createCheckoutSession(invitationId, planTier, billingInterval);
	}

	@Transactional
	public void cancelSubscription(final String externalSubscriptionId) {
		paymentProvider.cancelSubscription(externalSubscriptionId);
	}

}
