package com.nutriconsultas.subscription.payment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.invitation.SubscriptionProvisioningService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentWebhookService {

	private final PaymentProvider paymentProvider;

	private final PaymentWebhookEventRepository webhookEventRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final SubscriptionAuditEventRepository auditEventRepository;

	private final NutritionistInvitationRepository invitationRepository;

	private final SubscriptionProvisioningService provisioningService;

	public PaymentWebhookService(final PaymentProvider paymentProvider,
			final PaymentWebhookEventRepository webhookEventRepository,
			final SubscriptionRepository subscriptionRepository,
			final SubscriptionAuditEventRepository auditEventRepository,
			final NutritionistInvitationRepository invitationRepository,
			final SubscriptionProvisioningService provisioningService) {
		this.paymentProvider = paymentProvider;
		this.webhookEventRepository = webhookEventRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.auditEventRepository = auditEventRepository;
		this.invitationRepository = invitationRepository;
		this.provisioningService = provisioningService;
	}

	@Transactional
	public PaymentWebhookResult handleWebhook(final String payload, final PaymentWebhookHeaders headers) {
		if (!paymentProvider.verifyWebhookSignature(payload, headers)) {
			throw new InvalidPaymentWebhookException("Invalid payment webhook signature");
		}
		final ParsedPaymentWebhookEvent parsed = paymentProvider.parseWebhook(payload, headers);
		if (parsed.action() == PaymentWebhookAction.IGNORED) {
			return PaymentWebhookResult.ignored();
		}
		if (webhookEventRepository.findByProviderAndEventId(paymentProvider.getProviderId(), parsed.eventId())
			.isPresent()) {
			return PaymentWebhookResult.duplicate();
		}
		final Subscription subscription = resolveSubscription(parsed);
		if (subscription == null) {
			return PaymentWebhookResult.ignored();
		}
		applyWebhook(subscription, parsed);
		recordWebhookEvent(subscription, parsed);
		if (log.isInfoEnabled()) {
			log.info("Processed payment webhook: provider={}, eventId={}, subscriptionId={}, action={}",
					paymentProvider.getProviderId(), parsed.eventId(), subscription.getId(), parsed.action());
		}
		return PaymentWebhookResult.processed(subscription.getId());
	}

	private Subscription resolveSubscription(final ParsedPaymentWebhookEvent parsed) {
		if (!StringUtils.hasText(parsed.externalSubscriptionId())) {
			return null;
		}
		return subscriptionRepository.findByExternalSubscriptionId(parsed.externalSubscriptionId()).orElse(null);
	}

	private void applyWebhook(final Subscription subscription, final ParsedPaymentWebhookEvent parsed) {
		final SubscriptionStatus previousStatus = subscription.getStatus();
		if (parsed.action() == PaymentWebhookAction.PAYMENT_SUCCEEDED) {
			subscription.setStatus(SubscriptionStatus.ACTIVE);
			subscription.setPeriodStart(Instant.now());
			subscription.setPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
			if (StringUtils.hasText(parsed.externalCustomerId())) {
				subscription.setExternalCustomerId(parsed.externalCustomerId());
			}
			if (StringUtils.hasText(parsed.externalSubscriptionId())) {
				subscription.setExternalSubscriptionId(parsed.externalSubscriptionId());
			}
			recordAudit(subscription, previousStatus, SubscriptionAuditEventType.WEBHOOK_PAYMENT_SUCCEEDED,
					"Payment provider webhook");
			subscriptionRepository.save(subscription);
			provisionPaidInvitationAccess(subscription);
			return;
		}
		if (parsed.action() == PaymentWebhookAction.PAYMENT_FAILED) {
			subscription.setStatus(SubscriptionStatus.SUSPENDED);
			recordAudit(subscription, previousStatus, SubscriptionAuditEventType.WEBHOOK_PAYMENT_FAILED,
					"Payment provider webhook");
			subscriptionRepository.save(subscription);
			return;
		}
		if (parsed.action() == PaymentWebhookAction.SUBSCRIPTION_CANCELLED) {
			subscription.setStatus(SubscriptionStatus.CANCELLED);
			recordAudit(subscription, previousStatus, SubscriptionAuditEventType.STATE_TRANSITION,
					"Subscription cancelled by provider");
			subscriptionRepository.save(subscription);
		}
	}

	private void recordWebhookEvent(final Subscription subscription, final ParsedPaymentWebhookEvent parsed) {
		final PaymentWebhookEvent event = new PaymentWebhookEvent();
		event.setProvider(paymentProvider.getProviderId());
		event.setEventId(parsed.eventId());
		event.setEventType(parsed.eventType());
		event.setSubscription(subscription);
		webhookEventRepository.save(event);
	}

	private void recordAudit(final Subscription subscription, final SubscriptionStatus previousStatus,
			final SubscriptionAuditEventType eventType, final String details) {
		final SubscriptionAuditEvent auditEvent = new SubscriptionAuditEvent();
		auditEvent.setSubscription(subscription);
		auditEvent.setEventType(eventType);
		auditEvent.setPreviousStatus(previousStatus);
		auditEvent.setNewStatus(subscription.getStatus());
		auditEvent.setReasonCode("PAYMENT_PROVIDER");
		auditEvent.setDetails(details);
		auditEventRepository.save(auditEvent);
	}

	private void provisionPaidInvitationAccess(final Subscription subscription) {
		invitationRepository.findBySubscriptionId(subscription.getId()).ifPresent(invitation -> {
			if (invitation.getStatus() == InvitationStatus.REDEEMED) {
				provisioningService.activatePaidAccess(invitation, subscription);
			}
		});
	}

}
