package com.nutriconsultas.subscription.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.payment", name = "provider", havingValue = "mercadopago")
@ConditionalOnExpression("'${nutriconsultas.subscription.payment.mercadopago-access-token:}'.length() > 0")
@Slf4j
@Deprecated(since = "2.0", forRemoval = false)
public class MercadoPagoPaymentProvider implements PaymentProvider {

	private static final String API_BASE = "https://api.mercadopago.com";

	private final PaymentProperties paymentProperties;

	private final MercadoPagoWebhookSignatureVerifier signatureVerifier;

	private final NutritionistInvitationRepository invitationRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final RestClient restClient;

	private final ObjectMapper objectMapper;

	public MercadoPagoPaymentProvider(final PaymentProperties paymentProperties,
			final MercadoPagoWebhookSignatureVerifier signatureVerifier,
			final NutritionistInvitationRepository invitationRepository,
			final SubscriptionRepository subscriptionRepository, final RestClient.Builder restClientBuilder,
			final ObjectMapper objectMapper) {
		this.paymentProperties = paymentProperties;
		this.signatureVerifier = signatureVerifier;
		this.invitationRepository = invitationRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.restClient = restClientBuilder.build();
		this.objectMapper = objectMapper;
	}

	@Override
	public String getProviderId() {
		return PaymentProperties.PROVIDER_MERCADOPAGO;
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
		final Map<String, Object> body = buildPreapprovalBody(invitation, planTier, subscription.getId());
		final JsonNode response = postPreapproval(body);
		final String externalSubscriptionId = response.path("id").asText(null);
		final String checkoutUrl = response.path("init_point").asText(null);
		if (!StringUtils.hasText(externalSubscriptionId) || !StringUtils.hasText(checkoutUrl)) {
			throw new PaymentProviderException("Mercado Pago preapproval response missing checkout data");
		}
		subscription.setExternalSubscriptionId(externalSubscriptionId);
		subscriptionRepository.save(subscription);
		if (log.isInfoEnabled()) {
			log.info("Created Mercado Pago checkout: invitationId={}, subscriptionId={}, externalSubscriptionId={}",
					invitationId, subscription.getId(), externalSubscriptionId);
		}
		return new CheckoutSession(subscription.getId(), checkoutUrl, externalSubscriptionId, null);
	}

	@Override
	public boolean verifyWebhookSignature(final String payload, final PaymentWebhookHeaders headers) {
		return signatureVerifier.verify(headers);
	}

	@Override
	public ParsedPaymentWebhookEvent parseWebhook(final String payload, final PaymentWebhookHeaders headers) {
		if (!StringUtils.hasText(payload)) {
			throw new InvalidPaymentWebhookException("Webhook payload is required");
		}
		try {
			final JsonNode root = objectMapper.readTree(payload);
			final String eventType = root.path("type").asText("unknown");
			final String action = root.path("action").asText("");
			final String resourceId = root.path("data").path("id").asText(headers.dataId());
			if (!StringUtils.hasText(resourceId)) {
				throw new InvalidPaymentWebhookException("Webhook resource id is required");
			}
			final String eventId = buildEventId(headers.requestId(), resourceId, action);
			if ("subscription_preapproval".equals(eventType)) {
				return parsePreapprovalWebhook(eventId, eventType, resourceId);
			}
			if ("payment".equals(eventType)) {
				return parsePaymentWebhook(eventId, eventType, resourceId);
			}
			return new ParsedPaymentWebhookEvent(eventId, eventType, null, null, null, PaymentWebhookAction.IGNORED,
					null);
		}
		catch (InvalidPaymentWebhookException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new InvalidPaymentWebhookException("Unable to parse Mercado Pago webhook payload", ex);
		}
	}

	@Override
	public void cancelSubscription(final String externalSubscriptionId) {
		if (!StringUtils.hasText(externalSubscriptionId)) {
			throw new IllegalArgumentException("externalSubscriptionId is required");
		}
		final Map<String, Object> body = Map.of("status", "cancelled");
		restClient.put()
			.uri(API_BASE + "/preapproval/" + externalSubscriptionId)
			.header("Authorization", "Bearer " + paymentProperties.getMercadopagoAccessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.toBodilessEntity();
		if (log.isInfoEnabled()) {
			log.info("Cancelled Mercado Pago subscription: externalSubscriptionId={}", externalSubscriptionId);
		}
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

	private Map<String, Object> buildPreapprovalBody(final NutritionistInvitation invitation, final PlanTier planTier,
			final Long subscriptionId) {
		final Map<String, Object> autoRecurring = new HashMap<>();
		autoRecurring.put("frequency", 1);
		autoRecurring.put("frequency_type", "months");
		autoRecurring.put("transaction_amount", PaymentProperties.monthlyPriceMxn(planTier));
		autoRecurring.put("currency_id", paymentProperties.getCurrency());
		final Map<String, Object> body = new HashMap<>();
		body.put("reason", "Minutriporcion " + planTier.name());
		body.put("auto_recurring", autoRecurring);
		body.put("back_url", paymentProperties.getMercadopagoBackUrl());
		body.put("external_reference", "subscription:" + subscriptionId);
		body.put("payer_email", invitation.getEmail());
		return body;
	}

	private JsonNode postPreapproval(final Map<String, Object> body) {
		try {
			final String responseBody = restClient.post()
				.uri(API_BASE + "/preapproval")
				.header("Authorization", "Bearer " + paymentProperties.getMercadopagoAccessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(String.class);
			return objectMapper.readTree(responseBody);
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Mercado Pago preapproval request failed", ex);
		}
	}

	private ParsedPaymentWebhookEvent parsePreapprovalWebhook(final String eventId, final String eventType,
			final String preapprovalId) {
		final JsonNode preapproval = fetchPreapproval(preapprovalId);
		final String externalSubscriptionId = preapproval.path("id").asText(preapprovalId);
		final String status = preapproval.path("status").asText("");
		final String payerId = preapproval.path("payer_id").asText(null);
		if ("authorized".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, externalSubscriptionId, payerId, null,
					PaymentWebhookAction.PAYMENT_SUCCEEDED, SubscriptionStatus.ACTIVE);
		}
		if ("cancelled".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, externalSubscriptionId, payerId, null,
					PaymentWebhookAction.SUBSCRIPTION_CANCELLED, SubscriptionStatus.CANCELLED);
		}
		if ("paused".equalsIgnoreCase(status)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, externalSubscriptionId, payerId, null,
					PaymentWebhookAction.PAYMENT_FAILED, SubscriptionStatus.SUSPENDED);
		}
		return new ParsedPaymentWebhookEvent(eventId, eventType, externalSubscriptionId, payerId, null,
				PaymentWebhookAction.IGNORED, null);
	}

	private ParsedPaymentWebhookEvent parsePaymentWebhook(final String eventId, final String eventType,
			final String paymentId) {
		final JsonNode payment = fetchPayment(paymentId);
		final String status = payment.path("status").asText("");
		final String preapprovalId = payment.path("metadata").path("preapproval_id").asText(null);
		if (!"approved".equalsIgnoreCase(status) || !StringUtils.hasText(preapprovalId)) {
			return new ParsedPaymentWebhookEvent(eventId, eventType, preapprovalId, null, null,
					PaymentWebhookAction.IGNORED, null);
		}
		return new ParsedPaymentWebhookEvent(eventId, eventType, preapprovalId,
				payment.path("payer").path("id").asText(null), null, PaymentWebhookAction.PAYMENT_SUCCEEDED,
				SubscriptionStatus.ACTIVE);
	}

	private JsonNode fetchPreapproval(final String preapprovalId) {
		try {
			final String responseBody = restClient.get()
				.uri(API_BASE + "/preapproval/" + preapprovalId)
				.header("Authorization", "Bearer " + paymentProperties.getMercadopagoAccessToken())
				.retrieve()
				.body(String.class);
			return objectMapper.readTree(responseBody);
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Failed to fetch Mercado Pago preapproval " + preapprovalId, ex);
		}
	}

	private JsonNode fetchPayment(final String paymentId) {
		try {
			final String responseBody = restClient.get()
				.uri(API_BASE + "/v1/payments/" + paymentId)
				.header("Authorization", "Bearer " + paymentProperties.getMercadopagoAccessToken())
				.retrieve()
				.body(String.class);
			return objectMapper.readTree(responseBody);
		}
		catch (Exception ex) {
			throw new PaymentProviderException("Failed to fetch Mercado Pago payment " + paymentId, ex);
		}
	}

	private static String buildEventId(final String requestId, final String resourceId, final String action) {
		if (StringUtils.hasText(requestId)) {
			return requestId + ":" + resourceId;
		}
		return resourceId + ":" + action;
	}

}
