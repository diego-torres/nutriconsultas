package com.nutriconsultas.subscription.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.extern.slf4j.Slf4j;

/**
 * Stripe Checkout Sessions + Billing subscriptions for nutritionist paid invitations
 * (#207).
 */
@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.payment", name = "provider", havingValue = "stripe")
@ConditionalOnExpression("'${nutriconsultas.subscription.payment.stripe-secret-key:}'.length() > 0")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

	private final PaymentProperties paymentProperties;

	private final StripeClient stripeClient;

	private final NutritionistInvitationRepository invitationRepository;

	private final SubscriptionRepository subscriptionRepository;

	public StripePaymentProvider(final PaymentProperties paymentProperties,
			final NutritionistInvitationRepository invitationRepository,
			final SubscriptionRepository subscriptionRepository) {
		this.paymentProperties = paymentProperties;
		this.stripeClient = new StripeClient(paymentProperties.getStripeSecretKey());
		this.invitationRepository = invitationRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Override
	public String getProviderId() {
		return PaymentProperties.PROVIDER_STRIPE;
	}

	@Override
	public CheckoutSession createCheckoutSession(final Long invitationId, final PlanTier planTier,
			final BillingInterval billingInterval) {
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		if (planTier == null) {
			throw new IllegalArgumentException("planTier is required");
		}
		if (billingInterval != BillingInterval.MONTHLY) {
			throw new IllegalArgumentException("Only MONTHLY billing is supported in v1");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new PaymentProviderException("Invitation not found: " + invitationId));
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new PaymentProviderException("Invitation is not pending checkout");
		}
		final Subscription subscription = resolveSubscription(invitation, planTier);
		try {
			final SessionCreateParams params = buildSessionParams(invitation, invitationId, subscription.getId(),
					planTier);
			final Session session = stripeClient.checkout().sessions().create(params);
			final String checkoutUrl = session.getUrl();
			if (!StringUtils.hasText(checkoutUrl) || !StringUtils.hasText(session.getId())) {
				throw new PaymentProviderException("Stripe checkout session response missing checkout data");
			}
			subscription.setExternalSubscriptionId(session.getId());
			subscriptionRepository.save(subscription);
			if (log.isInfoEnabled()) {
				log.info("Created Stripe checkout: invitationId={}, subscriptionId={}, sessionId={}", invitationId,
						subscription.getId(), session.getId());
			}
			return new CheckoutSession(subscription.getId(), checkoutUrl, session.getId(), null);
		}
		catch (PaymentProviderException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Stripe checkout session request failed", ex);
		}
	}

	@Override
	public boolean verifyWebhookSignature(final String payload, final PaymentWebhookHeaders headers) {
		try {
			constructVerifiedEvent(payload, headers);
			return true;
		}
		catch (InvalidPaymentWebhookException ex) {
			return false;
		}
	}

	@Override
	public ParsedPaymentWebhookEvent parseWebhook(final String payload, final PaymentWebhookHeaders headers) {
		final Event event = constructVerifiedEvent(payload, headers);
		return StripeWebhookEventMapper.map(event);
	}

	@Override
	public void cancelSubscription(final String externalSubscriptionId) {
		if (!StringUtils.hasText(externalSubscriptionId)) {
			throw new IllegalArgumentException("externalSubscriptionId is required");
		}
		try {
			stripeClient.subscriptions().cancel(externalSubscriptionId, SubscriptionCancelParams.builder().build());
			if (log.isInfoEnabled()) {
				log.info("Cancelled Stripe subscription: externalSubscriptionId={}", externalSubscriptionId);
			}
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Failed to cancel Stripe subscription " + externalSubscriptionId, ex);
		}
	}

	private Event constructVerifiedEvent(final String payload, final PaymentWebhookHeaders headers) {
		if (!StringUtils.hasText(payload)) {
			throw new InvalidPaymentWebhookException("Webhook payload is required");
		}
		if (headers == null || !StringUtils.hasText(headers.signature())) {
			throw new InvalidPaymentWebhookException("Stripe-Signature header is required");
		}
		if (!StringUtils.hasText(paymentProperties.getStripeWebhookSecret())) {
			throw new InvalidPaymentWebhookException("Stripe webhook secret is not configured");
		}
		try {
			return stripeClient.constructEvent(payload, headers.signature(),
					paymentProperties.getStripeWebhookSecret());
		}
		catch (SignatureVerificationException ex) {
			throw new InvalidPaymentWebhookException("Invalid Stripe webhook signature", ex);
		}
		catch (Exception ex) {
			throw new InvalidPaymentWebhookException("Unable to parse Stripe webhook payload", ex);
		}
	}

	private SessionCreateParams buildSessionParams(final NutritionistInvitation invitation, final Long invitationId,
			final Long subscriptionId, final PlanTier planTier) {
		return SessionCreateParams.builder()
			.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
			.setCustomerEmail(invitation.getEmail())
			.setSuccessUrl(paymentProperties.getStripeSuccessUrl())
			.setCancelUrl(paymentProperties.getStripeCancelUrl())
			.putMetadata("subscription_id", String.valueOf(subscriptionId))
			.putMetadata("invitation_id", String.valueOf(invitationId))
			.addLineItem(buildLineItem(planTier))
			.build();
	}

	private SessionCreateParams.LineItem buildLineItem(final PlanTier planTier) {
		final String configuredPriceId = paymentProperties.resolveStripePriceId(planTier);
		if (StringUtils.hasText(configuredPriceId)) {
			return SessionCreateParams.LineItem.builder().setPrice(configuredPriceId).setQuantity(1L).build();
		}
		final long unitAmount = PaymentProperties.monthlyPriceMxn(planTier).movePointRight(2).longValueExact();
		return SessionCreateParams.LineItem.builder()
			.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
				.setCurrency(paymentProperties.getCurrency().toLowerCase())
				.setUnitAmount(unitAmount)
				.setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
					.setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
					.build())
				.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
					.setName("Minutriporcion " + planTier.getDisplayName())
					.build())
				.build())
			.setQuantity(1L)
			.build();
	}

	private Subscription resolveSubscription(final NutritionistInvitation invitation, final PlanTier planTier) {
		if (invitation.getSubscription() != null) {
			final Subscription existing = invitation.getSubscription();
			existing.setPlanTier(planTier);
			existing.setStatus(SubscriptionStatus.PENDING_PAYMENT);
			return subscriptionRepository.save(existing);
		}
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(planTier);
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		subscription.setPaymentExempt(false);
		subscription.setGracePeriodDays(7);
		final Subscription saved = subscriptionRepository.save(subscription);
		invitation.setSubscription(saved);
		invitationRepository.save(invitation);
		return saved;
	}

}
