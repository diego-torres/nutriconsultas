package com.nutriconsultas.subscription.payment;

/**
 * Provider-agnostic webhook headers used for signature verification.
 */
public record PaymentWebhookHeaders(String signature, String requestId, String dataId) {
}
