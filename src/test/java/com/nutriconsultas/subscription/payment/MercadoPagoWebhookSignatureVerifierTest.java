package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookSignatureVerifierTest {

	private PaymentProperties paymentProperties;

	private MercadoPagoWebhookSignatureVerifier verifier;

	@BeforeEach
	void setUp() {
		paymentProperties = new PaymentProperties();
		paymentProperties.setWebhookSecret("test-webhook-secret");
		verifier = new MercadoPagoWebhookSignatureVerifier(paymentProperties);
	}

	@Test
	void verifyAcceptsValidSignature() {
		final PaymentWebhookHeaders headers = new PaymentWebhookHeaders(
				"ts=1704908010,v1=618c8534524d296c3a61d7267e4d516b", "abc-123", "12345");
		final String manifest = "id:12345;request-id:abc-123;ts:1704908010;";
		final String expected = hmacSha256Hex("test-webhook-secret", manifest);
		final PaymentWebhookHeaders validHeaders = new PaymentWebhookHeaders("ts=1704908010,v1=" + expected, "abc-123",
				"12345");

		assertThat(verifier.verify(validHeaders)).isTrue();
		assertThat(verifier.verify(headers)).isFalse();
	}

	@Test
	void verifyRejectsMissingSecret() {
		paymentProperties.setWebhookSecret("");
		assertThat(verifier.verify(new PaymentWebhookHeaders("ts=1,v1=abc", "req", "1"))).isFalse();
	}

	@Test
	void verifyRejectsMissingHeaders() {
		assertThat(verifier.verify(new PaymentWebhookHeaders(null, "req", "1"))).isFalse();
		assertThat(verifier.verify(new PaymentWebhookHeaders("ts=1,v1=abc", null, "1"))).isFalse();
		assertThat(verifier.verify(new PaymentWebhookHeaders("ts=1,v1=abc", "req", null))).isFalse();
	}

	private static String hmacSha256Hex(final String secret, final String manifest) {
		try {
			final javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
			mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
					"HmacSHA256"));
			return java.util.HexFormat.of()
				.formatHex(mac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
