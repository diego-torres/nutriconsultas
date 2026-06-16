package com.nutriconsultas.subscription.payment;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mercado Pago webhook signature verification (x-signature manifest + HMAC-SHA256).
 */
@Component
public final class MercadoPagoWebhookSignatureVerifier {

	private final PaymentProperties paymentProperties;

	public MercadoPagoWebhookSignatureVerifier(final PaymentProperties paymentProperties) {
		this.paymentProperties = paymentProperties;
	}

	public boolean verify(final PaymentWebhookHeaders headers) {
		if (!StringUtils.hasText(paymentProperties.getWebhookSecret())) {
			return false;
		}
		if (headers == null || !StringUtils.hasText(headers.signature()) || !StringUtils.hasText(headers.requestId())
				|| !StringUtils.hasText(headers.dataId())) {
			return false;
		}
		final String timestamp = extractPart(headers.signature(), "ts");
		final String providedHash = extractPart(headers.signature(), "v1");
		if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(providedHash)) {
			return false;
		}
		final String manifest = "id:" + headers.dataId() + ";request-id:" + headers.requestId() + ";ts:" + timestamp
				+ ";";
		final String expectedHash = hmacSha256Hex(paymentProperties.getWebhookSecret(), manifest);
		return constantTimeEquals(expectedHash, providedHash);
	}

	private static String extractPart(final String signatureHeader, final String key) {
		final String prefix = key + "=";
		for (final String part : signatureHeader.split(",")) {
			final String trimmed = part.trim();
			if (trimmed.startsWith(prefix)) {
				return trimmed.substring(prefix.length());
			}
		}
		return null;
	}

	private static String hmacSha256Hex(final String secret, final String manifest) {
		try {
			final Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Failed to verify Mercado Pago webhook signature", ex);
		}
	}

	private static boolean constantTimeEquals(final String left, final String right) {
		if (left == null || right == null || left.length() != right.length()) {
			return false;
		}
		int result = 0;
		for (int index = 0; index < left.length(); index++) {
			result |= left.charAt(index) ^ right.charAt(index);
		}
		return result == 0;
	}

}
