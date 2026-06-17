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

import com.nutriconsultas.platform.PlatformAdminService;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.payment.BillingInterval;
import com.nutriconsultas.subscription.payment.CheckoutSession;
import com.nutriconsultas.subscription.payment.PaymentCheckoutService;
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

	public NutritionistInvitationServiceImpl(final PlatformAdminService platformAdminService,
			final NutritionistInvitationRepository invitationRepository,
			final NutritionistInvitationProperties invitationProperties,
			final InvitationEmailSender invitationEmailSender,
			final SubscriptionProvisioningService provisioningService,
			final PaymentCheckoutService paymentCheckoutService,
			final SubscriptionAuditEventRepository auditEventRepository) {
		this.platformAdminService = platformAdminService;
		this.invitationRepository = invitationRepository;
		this.invitationProperties = invitationProperties;
		this.invitationEmailSender = invitationEmailSender;
		this.provisioningService = provisioningService;
		this.paymentCheckoutService = paymentCheckoutService;
		this.auditEventRepository = auditEventRepository;
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
				throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending invitation already exists for email");
			});
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
		final CheckoutSession checkout = paymentCheckoutService.createCheckoutSession(invitation.getId(),
				invitation.getPlanTier(), BillingInterval.MONTHLY);
		return new RedeemNutritionistInvitationResult.CheckoutRedirect(checkout.checkoutUrl());
	}

	NutritionistInvitation findValidInvitation(final String rawToken) {
		final String tokenHash = InvitationTokenHasher.hashToken(rawToken);
		final NutritionistInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
		if (invitation.getStatus() == InvitationStatus.REDEEMED) {
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation already redeemed");
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
