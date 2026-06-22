package com.nutriconsultas.subscription.payment;

import org.springframework.util.StringUtils;

/**
 * Provider-agnostic webhook headers used for signature verification.
 */
public record PaymentWebhookHeaders(String signature, String requestId, String dataId) {

	public static PaymentWebhookHeaders fromServletRequest(final jakarta.servlet.http.HttpServletRequest request) {
		final String stripeSignature = request.getHeader("Stripe-Signature");
		final String signature = StringUtils.hasText(stripeSignature) ? stripeSignature
				: request.getHeader("x-signature");
		return new PaymentWebhookHeaders(signature, request.getHeader("x-request-id"), request.getParameter("data.id"));
	}

}
