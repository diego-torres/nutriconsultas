package com.nutriconsultas.subscription.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.payment.PaymentProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Simulates a successful payment after stub checkout redirect (local dev without Mercado
 * Pago).
 */
@Service
@Slf4j
public class NutritionistInvitationDevCheckoutService {

	private final PaymentProperties paymentProperties;

	private final NutritionistInvitationRepository invitationRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final SubscriptionAuditEventRepository auditEventRepository;

	private final SubscriptionProvisioningService provisioningService;

	public NutritionistInvitationDevCheckoutService(final PaymentProperties paymentProperties,
			final NutritionistInvitationRepository invitationRepository,
			final SubscriptionRepository subscriptionRepository,
			final SubscriptionAuditEventRepository auditEventRepository,
			final SubscriptionProvisioningService provisioningService) {
		this.paymentProperties = paymentProperties;
		this.invitationRepository = invitationRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.auditEventRepository = auditEventRepository;
		this.provisioningService = provisioningService;
	}

	@Transactional
	public void completeStubCheckout(final OidcUser principal, final Long invitationId) {
		requireStubSimulateEnabled();
		if (principal == null || !StringUtils.hasText(principal.getSubject())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() != InvitationStatus.REDEEMED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación no está lista para activar el pago");
		}
		if (!principal.getSubject().equals(invitation.getRedeemedByUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el usuario invitado puede completar el pago");
		}
		final Subscription subscription = invitation.getSubscription();
		if (subscription == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación no tiene suscripción asociada");
		}
		if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La suscripción ya fue activada");
		}
		final SubscriptionStatus previousStatus = subscription.getStatus();
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setPeriodStart(Instant.now());
		subscription.setPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
		subscription.setExternalSubscriptionId("stub-sub-" + invitationId);
		subscription.setExternalCustomerId("stub-cust-" + invitationId);
		subscriptionRepository.save(subscription);
		recordStubPaymentAudit(subscription, previousStatus, principal.getSubject());
		provisioningService.activatePaidAccess(invitation, subscription);
		if (log.isInfoEnabled()) {
			log.info("Completed stub checkout: invitationId={}, subscriptionId={}", invitationId, subscription.getId());
		}
	}

	private void requireStubSimulateEnabled() {
		if (!paymentProperties.isStubSimulateCheckout() || paymentProperties.isMercadoPagoConfigured()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	private void recordStubPaymentAudit(final Subscription subscription, final SubscriptionStatus previousStatus,
			final String actorUserId) {
		final SubscriptionAuditEvent auditEvent = new SubscriptionAuditEvent();
		auditEvent.setSubscription(subscription);
		auditEvent.setEventType(SubscriptionAuditEventType.WEBHOOK_PAYMENT_SUCCEEDED);
		auditEvent.setActorUserId(actorUserId);
		auditEvent.setPreviousStatus(previousStatus);
		auditEvent.setNewStatus(SubscriptionStatus.ACTIVE);
		auditEvent.setReasonCode("STUB_CHECKOUT");
		auditEvent.setDetails("provider=stub");
		auditEventRepository.save(auditEvent);
	}

}
