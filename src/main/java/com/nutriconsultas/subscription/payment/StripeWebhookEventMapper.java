package com.nutriconsultas.subscription.payment;

import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.SubscriptionStatus;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

/**
 * Maps verified Stripe webhook {@link Event} objects to normalized payment actions
 * (#207).
 */
public final class StripeWebhookEventMapper {

	private StripeWebhookEventMapper() {
	}

	public static ParsedPaymentWebhookEvent map(final Event event) {
		final String eventId = event.getId();
		final String eventType = event.getType();
		return switch (eventType) {
			case "checkout.session.completed" -> mapCheckoutSessionCompleted(eventId, eventType, event);
			case "customer.subscription.updated" -> mapSubscriptionUpdated(eventId, eventType, event);
			case "customer.subscription.deleted" -> mapSubscriptionDeleted(eventId, eventType, event);
			case "invoice.payment_failed" -> mapInvoicePaymentFailed(eventId, eventType, event);
			default -> ignored(eventId, eventType);
		};
	}

	private static ParsedPaymentWebhookEvent mapCheckoutSessionCompleted(final String eventId, final String eventType,
			final Event event) {
		final Session session = deserialize(event, Session.class);
		if (session == null || !"complete".equalsIgnoreCase(session.getStatus())) {
			return ignored(eventId, eventType);
		}
		final String lookupId = session.getId();
		final String providerSubscriptionId = session.getSubscription();
		final String customerId = session.getCustomer();
		if (!StringUtils.hasText(lookupId)) {
			return ignored(eventId, eventType);
		}
		return new ParsedPaymentWebhookEvent(eventId, eventType, lookupId, customerId, providerSubscriptionId,
				PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE);
	}

	private static ParsedPaymentWebhookEvent mapSubscriptionUpdated(final String eventId, final String eventType,
			final Event event) {
		final Subscription subscription = deserialize(event, Subscription.class);
		if (subscription == null || !StringUtils.hasText(subscription.getId())) {
			return ignored(eventId, eventType);
		}
		final String status = subscription.getStatus();
		if ("active".equalsIgnoreCase(status) || "trialing".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, subscription.getId(), subscription.getCustomer(),
					null, PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE);
		}
		if ("past_due".equalsIgnoreCase(status) || "unpaid".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, subscription.getId(), subscription.getCustomer(),
					null, PaymentWebhookAction.PAYMENT_FAILED, SubscriptionStatus.SUSPENDED);
		}
		if ("canceled".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, subscription.getId(), subscription.getCustomer(),
					null, PaymentWebhookAction.SUBSCRIPTION_CANCELLED, SubscriptionStatus.CANCELLED);
		}
		return ignored(eventId, eventType);
	}

	private static ParsedPaymentWebhookEvent mapSubscriptionDeleted(final String eventId, final String eventType,
			final Event event) {
		final Subscription subscription = deserialize(event, Subscription.class);
		if (subscription == null || !StringUtils.hasText(subscription.getId())) {
			return ignored(eventId, eventType);
		}
		return new ParsedPaymentWebhookEvent(eventId, eventType, subscription.getId(), subscription.getCustomer(), null,
				PaymentWebhookAction.SUBSCRIPTION_CANCELLED, SubscriptionStatus.CANCELLED);
	}

	private static ParsedPaymentWebhookEvent mapInvoicePaymentFailed(final String eventId, final String eventType,
			final Event event) {
		final Invoice invoice = deserialize(event, Invoice.class);
		final String subscriptionId = resolveInvoiceSubscriptionId(invoice);
		if (invoice == null || !StringUtils.hasText(subscriptionId)) {
			return ignored(eventId, eventType);
		}
		return new ParsedPaymentWebhookEvent(eventId, eventType, subscriptionId, invoice.getCustomer(), null,
				PaymentWebhookAction.PAYMENT_FAILED, SubscriptionStatus.SUSPENDED);
	}

	private static String resolveInvoiceSubscriptionId(final Invoice invoice) {
		if (invoice == null || invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
			return null;
		}
		return invoice.getParent().getSubscriptionDetails().getSubscription();
	}

	private static ParsedPaymentWebhookEvent ignored(final String eventId, final String eventType) {
		return new ParsedPaymentWebhookEvent(eventId, eventType, null, null, null, PaymentWebhookAction.IGNORED, null);
	}

	private static <T extends StripeObject> T deserialize(final Event event, final Class<T> type) {
		try {
			final StripeObject stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
			if (stripeObject != null && type.isInstance(stripeObject)) {
				return type.cast(stripeObject);
			}
		}
		catch (Exception ex) {
			throw new InvalidPaymentWebhookException("Unable to deserialize Stripe webhook object", ex);
		}
		return null;
	}

}
