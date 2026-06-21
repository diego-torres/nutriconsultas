package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.SubscriptionStatus;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;

class StripeWebhookEventMapperTest {

	@Test
	void mapsCheckoutSessionCompletedToPaymentSucceeded() {
		final Event event = ApiResource.GSON.fromJson("""
				{
				  "id": "evt_checkout_1",
				  "type": "checkout.session.completed",
				  "data": {
				    "object": {
				      "id": "cs_test_1",
				      "object": "checkout.session",
				      "status": "complete",
				      "subscription": "sub_test_1",
				      "customer": "cus_test_1"
				    }
				  }
				}
				""", Event.class);

		final ParsedPaymentWebhookEvent parsed = StripeWebhookEventMapper.map(event);

		assertThat(parsed.eventId()).isEqualTo("evt_checkout_1");
		assertThat(parsed.eventType()).isEqualTo("checkout.session.completed");
		assertThat(parsed.externalSubscriptionId()).isEqualTo("cs_test_1");
		assertThat(parsed.providerSubscriptionId()).isEqualTo("sub_test_1");
		assertThat(parsed.externalCustomerId()).isEqualTo("cus_test_1");
		assertThat(parsed.action()).isEqualTo(PaymentWebhookAction.PAYMENT_SUCCEEDED);
		assertThat(parsed.targetStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
	}

	@Test
	void mapsSubscriptionDeletedToCancelled() {
		final Event event = ApiResource.GSON.fromJson("""
				{
				  "id": "evt_sub_del",
				  "type": "customer.subscription.deleted",
				  "data": {
				    "object": {
				      "id": "sub_test_1",
				      "object": "subscription",
				      "status": "canceled",
				      "customer": "cus_test_1"
				    }
				  }
				}
				""", Event.class);

		final ParsedPaymentWebhookEvent parsed = StripeWebhookEventMapper.map(event);

		assertThat(parsed.action()).isEqualTo(PaymentWebhookAction.SUBSCRIPTION_CANCELLED);
		assertThat(parsed.externalSubscriptionId()).isEqualTo("sub_test_1");
		assertThat(parsed.providerSubscriptionId()).isNull();
	}

	@Test
	void ignoresUnknownEventTypes() {
		final Event event = ApiResource.GSON.fromJson("""
				{
				  "id": "evt_unknown",
				  "type": "payment_intent.succeeded",
				  "data": { "object": { "id": "pi_1", "object": "payment_intent" } }
				}
				""", Event.class);

		final ParsedPaymentWebhookEvent parsed = StripeWebhookEventMapper.map(event);

		assertThat(parsed.action()).isEqualTo(PaymentWebhookAction.IGNORED);
	}

}
