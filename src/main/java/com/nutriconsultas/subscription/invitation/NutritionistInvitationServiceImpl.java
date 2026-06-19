package com.nutriconsultas.subscription.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.platform.PlatformAdminService;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.payment.BillingInterval;
import com.nutriconsultas.subscription.payment.CheckoutSession;
import com.nutriconsultas.subscription.payment.PaymentCheckoutService;
import com.nutriconsultas.subscription.payment.PaymentProviderException;
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistInvitationServiceImpl implements NutritionistInvitationService {

	private final PlatformAdminService platformAdminService;

	private final NutritionistInvitationRepository invitationRepository;

	private final NutritionistInvitationProperties invitationProperties;

	private final InvitationEmailSender invitationEmailSender;

	private final SubscriptionProvisioningService provisioningService;

	private final PaymentCheckoutService paymentCheckoutService;

	private final SubscriptionAuditEventRepository auditEventRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final Auth0RoleSyncClient auth0RoleSyncClient;

	public NutritionistInvitationServiceImpl(final PlatformAdminService platformAdminService,
			final NutritionistInvitationRepository invitationRepository,
			final NutritionistInvitationProperties invitationProperties,
			final InvitationEmailSender invitationEmailSender,
			final SubscriptionProvisioningService provisioningService,
			final PaymentCheckoutService paymentCheckoutService,
			final SubscriptionAuditEventRepository auditEventRepository,
			final SubscriptionRepository subscriptionRepository, final Auth0RoleSyncClient auth0RoleSyncClient) {
		this.platformAdminService = platformAdminService;
		this.invitationRepository = invitationRepository;
		this.invitationProperties = invitationProperties;
		this.invitationEmailSender = invitationEmailSender;
		this.provisioningService = provisioningService;
		this.paymentCheckoutService = paymentCheckoutService;
		this.auditEventRepository = auditEventRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.auth0RoleSyncClient = auth0RoleSyncClient;
	}

	@Override
	@Transactional
	public CreatedNutritionistInvitation createInvitation(final OidcUser adminPrincipal, final String email,
			final PlanTier planTier, final boolean paymentExempt) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		final String normalizedEmail = normalizeEmail(email);
		if (planTier == null) {
			throw new IllegalArgumentException("planTier is required");
		}
		invitationRepository.findByEmailIgnoreCaseAndStatus(normalizedEmail, InvitationStatus.PENDING)
			.ifPresent(existing -> {
				throw new PendingNutritionistInvitationException(existing.getId());
			});
		rejectIfActiveNutritionist(normalizedEmail);
		final String rawToken = InvitationTokenHasher.generateToken();
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setEmail(normalizedEmail);
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		invitation.setPlanTier(planTier);
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS));
		invitation.setCreatedByUserId(platformAdminService.resolveActorUserId(adminPrincipal));
		invitation.setPaymentExempt(paymentExempt);
		final NutritionistInvitation saved = invitationRepository.save(invitation);
		final String inviteUrl = invitationProperties.buildRedeemUrl(rawToken);
		invitationEmailSender.sendNutritionistInvitation(normalizedEmail, planTier, inviteUrl);
		recordAdminInvitationAudit(platformAdminService.resolveActorUserId(adminPrincipal), saved.getId(), planTier,
				paymentExempt);
		if (log.isInfoEnabled()) {
			log.info("Created nutritionist invitation: invitationId={}, planTier={}, paymentExempt={}", saved.getId(),
					planTier, paymentExempt);
		}
		return new CreatedNutritionistInvitation(saved.getId(), inviteUrl);
	}

	@Override
	@Transactional
	public void cancelInvitation(final OidcUser adminPrincipal, final Long invitationId) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar invitaciones pendientes");
		}
		invitation.setStatus(InvitationStatus.CANCELLED);
		invitationRepository.save(invitation);
		recordInvitationCancelAudit(platformAdminService.resolveActorUserId(adminPrincipal), invitation.getId());
		if (log.isInfoEnabled()) {
			log.info("Cancelled nutritionist invitation: invitationId={}", invitation.getId());
		}
	}

	@Override
	@Transactional
	public void revokeNutritionistAccess(final OidcUser adminPrincipal, final Long invitationId, final String reason) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() != InvitationStatus.REDEEMED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se puede revocar acceso de invitaciones aceptadas");
		}
		final Subscription subscription = invitation.getSubscription();
		if (subscription == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación no tiene suscripción asociada");
		}
		if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El acceso ya fue revocado");
		}
		if (!StringUtils.hasText(invitation.getRedeemedByUserId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación no tiene usuario asociado");
		}
		final String actorUserId = platformAdminService.resolveActorUserId(adminPrincipal);
		final String targetUserId = invitation.getRedeemedByUserId();
		final SubscriptionStatus previousStatus = subscription.getStatus();
		final String paymentCancelDetail = cancelExternalSubscriptionIfPresent(subscription);
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		subscriptionRepository.save(subscription);
		final String auth0Detail = revokeAuth0RolesIfConfigured(targetUserId);
		recordAccessRevokeAudit(actorUserId, invitation, subscription, previousStatus, reason, paymentCancelDetail,
				auth0Detail);
		if (log.isInfoEnabled()) {
			log.info("Revoked nutritionist access: invitationId={}, subscriptionId={}, targetUserId={}",
					invitation.getId(), subscription.getId(), targetUserId);
		}
	}

	@Override
	@Transactional
	public String regenerateInvitationLink(final OidcUser adminPrincipal, final Long invitationId) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final NutritionistInvitation invitation = invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se puede obtener el enlace de invitaciones pendientes");
		}
		final String rawToken = InvitationTokenHasher.generateToken();
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		if (Instant.now().isAfter(invitation.getExpiresAt())) {
			invitation.setExpiresAt(Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS));
		}
		invitationRepository.save(invitation);
		final String inviteUrl = invitationProperties.buildRedeemUrl(rawToken);
		recordInvitationLinkRegeneratedAudit(platformAdminService.resolveActorUserId(adminPrincipal),
				invitation.getId());
		if (log.isInfoEnabled()) {
			log.info("Regenerated nutritionist invitation link: invitationId={}", invitation.getId());
		}
		return inviteUrl;
	}

	@Override
	@Transactional
	public RedeemNutritionistInvitationResult redeemInvitation(final OidcUser principal, final String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			throw new IllegalArgumentException("token is required");
		}
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		final NutritionistInvitation invitation = findValidInvitation(rawToken);
		verifyEmailMatches(principal, invitation);
		markRedeemed(invitation, principal.getSubject());
		if (invitation.isPaymentExempt()) {
			provisioningService.activateTrialAccess(invitation, principal.getSubject());
			return new RedeemNutritionistInvitationResult.Activated();
		}
		provisioningService.createPendingSubscription(invitation);
		invitationRepository.save(invitation);
		try {
			final CheckoutSession checkout = paymentCheckoutService.createCheckoutSession(invitation.getId(),
					invitation.getPlanTier(), BillingInterval.MONTHLY);
			return new RedeemNutritionistInvitationResult.CheckoutRedirect(checkout.checkoutUrl());
		}
		catch (PaymentProviderException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"El pago en línea no está configurado. Solicite una invitación exenta de pago al administrador.");
		}
	}

	NutritionistInvitation findValidInvitation(final String rawToken) {
		final String tokenHash = InvitationTokenHasher.hashToken(rawToken);
		final NutritionistInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() == InvitationStatus.REDEEMED) {
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation already redeemed");
		}
		if (invitation.getStatus() == InvitationStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation was cancelled");
		}
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation is no longer valid");
		}
		if (Instant.now().isAfter(invitation.getExpiresAt())) {
			invitation.setStatus(InvitationStatus.EXPIRED);
			invitationRepository.save(invitation);
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation has expired");
		}
		return invitation;
	}

	private void verifyEmailMatches(final OidcUser principal, final NutritionistInvitation invitation) {
		final String principalEmail = principal.getEmail();
		if (!StringUtils.hasText(principalEmail)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Your account must have a verified email to redeem this invitation");
		}
		if (!normalizeEmail(principalEmail).equals(invitation.getEmail())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Sign in with the email address that received the invitation");
		}
	}

	private void markRedeemed(final NutritionistInvitation invitation, final String userId) {
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setRedeemedAt(Instant.now());
		invitation.setRedeemedByUserId(userId);
		invitationRepository.save(invitation);
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(invitation.getSubscription());
		event.setEventType(SubscriptionAuditEventType.INVITATION_REDEEMED);
		event.setActorUserId(userId);
		event.setDetails("invitationId=" + invitation.getId());
		auditEventRepository.save(event);
	}

	private void rejectIfActiveNutritionist(final String normalizedEmail) {
		invitationRepository
			.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(normalizedEmail, InvitationStatus.REDEEMED)
			.filter(this::hasActiveSubscriptionAccess)
			.ifPresent(redeemed -> {
				throw new ActiveNutritionistUserException(redeemed.getId());
			});
	}

	private boolean hasActiveSubscriptionAccess(final NutritionistInvitation redeemedInvitation) {
		final Subscription subscription = redeemedInvitation.getSubscription();
		return subscription != null && NutritionistInvitationAccessRules.blocksNewInvitation(subscription.getStatus());
	}

	private String cancelExternalSubscriptionIfPresent(final Subscription subscription) {
		if (!StringUtils.hasText(subscription.getExternalSubscriptionId())) {
			return "externalSubscriptionCancelled=skipped";
		}
		try {
			paymentCheckoutService.cancelSubscription(subscription.getExternalSubscriptionId());
			return "externalSubscriptionCancelled=true";
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("External subscription cancel failed: subscriptionId={}", subscription.getId());
			}
			if (log.isDebugEnabled()) {
				log.debug("External subscription cancel failure", ex);
			}
			return "externalSubscriptionCancelled=false";
		}
	}

	private String revokeAuth0RolesIfConfigured(final String targetUserId) {
		if (!auth0RoleSyncClient.isConfigured()) {
			return "auth0Revoked=skipped";
		}
		try {
			auth0RoleSyncClient.revokePlanRoles(targetUserId);
			return "auth0Revoked=true";
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role revoke failed after subscription cancel: targetUserId present={}",
						StringUtils.hasText(targetUserId));
			}
			if (log.isDebugEnabled()) {
				log.debug("Auth0 role revoke failure", ex);
			}
			return "auth0Revoked=false";
		}
	}

	private void recordAccessRevokeAudit(final String actorUserId, final NutritionistInvitation invitation,
			final Subscription subscription, final SubscriptionStatus previousStatus, final String reason,
			final String paymentCancelDetail, final String auth0Detail) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setActorUserId(actorUserId);
		event.setPreviousStatus(previousStatus);
		event.setNewStatus(SubscriptionStatus.CANCELLED);
		final String reasonSuffix = StringUtils.hasText(reason) ? ",reason=" + reason.trim() : "";
		event.setDetails("action=access.revoke,invitationId=" + invitation.getId() + ",targetUserId="
				+ invitation.getRedeemedByUserId() + ",email=" + invitation.getEmail() + reasonSuffix + ","
				+ paymentCancelDetail + "," + auth0Detail);
		auditEventRepository.save(event);
	}

	private void recordInvitationLinkRegeneratedAudit(final String actorUserId, final Long invitationId) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setActorUserId(actorUserId);
		event.setDetails("action=invitation.regenerate-link,invitationId=" + invitationId);
		auditEventRepository.save(event);
	}

	private void recordInvitationCancelAudit(final String actorUserId, final Long invitationId) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setActorUserId(actorUserId);
		event.setDetails("action=invitation.cancel,invitationId=" + invitationId);
		auditEventRepository.save(event);
	}

	private void recordAdminInvitationAudit(final String actorUserId, final Long invitationId, final PlanTier planTier,
			final boolean paymentExempt) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setActorUserId(actorUserId);
		event.setDetails("action=invitation.create,invitationId=" + invitationId + ",planTier=" + planTier
				+ ",paymentExempt=" + paymentExempt);
		auditEventRepository.save(event);
	}

	private static String normalizeEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
