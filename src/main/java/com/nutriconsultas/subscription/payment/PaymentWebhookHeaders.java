package com.nutriconsultas.subscription.payment;

/**
 * Provider-agnostic webhook headers used for signature verification.
 */
public record PaymentWebhookHeaders(String signature, String requestId, String dataId) {

	public static PaymentWebhookHeaders fromServletRequest(final jakarta.servlet.http.HttpServletRequest request) {
		return new PaymentWebhookHeaders(request.getHeader("x-signature"), request.getHeader("x-request-id"),
				request.getParameter("data.id"));
	}

}
