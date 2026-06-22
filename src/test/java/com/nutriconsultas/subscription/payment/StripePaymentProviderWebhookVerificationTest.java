package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripePaymentProviderWebhookVerificationTest {

	private static final String WEBHOOK_SECRET = "whsec_unit_test_secret";

	private StripePaymentProvider provider;

	@BeforeEach
	void setUp() {
		final PaymentProperties properties = new PaymentProperties();
		properties.setProvider(PaymentProperties.PROVIDER_STRIPE);
		properties.setStripeSecretKey("sk_test_unit");
		properties.setStripeWebhookSecret(WEBHOOK_SECRET);
		provider = new StripePaymentProvider(properties, null, null);
	}

	@Test
	void verifyWebhookSignatureAcceptsSignedPayload() throws Exception {
		final String payload = """
				{
				  "id": "evt_verify_1",
				  "type": "checkout.session.completed",
				  "data": {
				    "object": {
				      "id": "cs_verify",
				      "object": "checkout.session",
				      "status": "complete",
				      "subscription": "sub_verify",
				      "customer": "cus_verify"
				    }
				  }
				}
				""";
		final String signature = signStripePayload(payload, WEBHOOK_SECRET);
		final PaymentWebhookHeaders headers = new PaymentWebhookHeaders(signature, null, null);

		assertThat(provider.verifyWebhookSignature(payload, headers)).isTrue();
	}

	@Test
	void verifyWebhookSignatureRejectsTamperedPayload() {
		final String payload = "{\"id\":\"evt_bad\"}";
		final PaymentWebhookHeaders headers = new PaymentWebhookHeaders("invalid", null, null);

		assertThat(provider.verifyWebhookSignature(payload, headers)).isFalse();
	}

	private static String signStripePayload(final String payload, final String secret) throws Exception {
		final long timestamp = Instant.now().getEpochSecond();
		final String signedPayload = timestamp + "." + payload;
		final Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		final String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
		return "t=" + timestamp + ",v1=" + signature;
	}

}
