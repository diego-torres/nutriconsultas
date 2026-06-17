package com.nutriconsultas.subscription.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates clinic membership and syncs Auth0 roles after invitation redemption or payment.
 */
@Service
@Slf4j
public class SubscriptionProvisioningService {

	private static final int TRIAL_PERIOD_DAYS = 30;

	private final ClinicRepository clinicRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final Auth0RoleSyncClient auth0RoleSyncClient;

	private final SubscriptionAuditEventRepository auditEventRepository;

	public SubscriptionProvisioningService(final ClinicRepository clinicRepository,
			final ClinicMemberRepository clinicMemberRepository, final SubscriptionRepository subscriptionRepository,
			final Auth0RoleSyncClient auth0RoleSyncClient,
			final SubscriptionAuditEventRepository auditEventRepository) {
		this.clinicRepository = clinicRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.auth0RoleSyncClient = auth0RoleSyncClient;
		this.auditEventRepository = auditEventRepository;
	}

	@Transactional
	public void activateTrialAccess(final NutritionistInvitation invitation, final String userId) {
		final Subscription subscription = ensureSubscription(invitation, SubscriptionStatus.TRIAL, true);
		setTrialPeriod(subscription);
		subscriptionRepository.save(subscription);
		provisionClinicAccess(userId, invitation.getPlanTier(), subscription, invitation.getCreatedByUserId());
		syncAuth0RoleIfConfigured(userId, invitation.getPlanTier());
	}

	@Transactional
	public void activatePaidAccess(final NutritionistInvitation invitation, final Subscription subscription) {
		if (!StringUtils.hasText(invitation.getRedeemedByUserId())) {
			if (log.isWarnEnabled()) {
				log.warn("Skipping provisioning: invitationId={} has no redeemed user", invitation.getId());
			}
			return;
		}
		provisionClinicAccess(invitation.getRedeemedByUserId(), subscription.getPlanTier(), subscription,
				invitation.getCreatedByUserId());
		syncAuth0RoleIfConfigured(invitation.getRedeemedByUserId(), subscription.getPlanTier());
	}

	@Transactional
	public Subscription createPendingSubscription(final NutritionistInvitation invitation) {
		return ensureSubscription(invitation, SubscriptionStatus.PENDING_PAYMENT, false);
	}

	private Subscription ensureSubscription(final NutritionistInvitation invitation, final SubscriptionStatus status,
			final boolean paymentExempt) {
		if (invitation.getSubscription() != null) {
			final Subscription existing = invitation.getSubscription();
			existing.setPlanTier(invitation.getPlanTier());
			existing.setStatus(status);
			existing.setPaymentExempt(paymentExempt);
			return subscriptionRepository.save(existing);
		}
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(invitation.getPlanTier());
		subscription.setStatus(status);
		subscription.setPaymentExempt(paymentExempt);
		subscription.setGracePeriodDays(7);
		final Subscription saved = subscriptionRepository.save(subscription);
		invitation.setSubscription(saved);
		return saved;
	}

	private void setTrialPeriod(final Subscription subscription) {
		subscription.setPeriodStart(Instant.now());
		subscription.setPeriodEnd(Instant.now().plus(TRIAL_PERIOD_DAYS, ChronoUnit.DAYS));
	}

	private void provisionClinicAccess(final String userId, final PlanTier planTier, final Subscription subscription,
			final String invitedByUserId) {
		if (clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId).isPresent()) {
			return;
		}
		if (clinicRepository.findByDirectorUserId(userId).isPresent()) {
			return;
		}
		final Clinic clinic = new Clinic();
		clinic.setName("Minutriporción");
		clinic.setDirectorUserId(userId);
		clinic.setSubscription(subscription);
		final Clinic savedClinic = clinicRepository.save(clinic);
		final ClinicMember member = new ClinicMember();
		member.setClinic(savedClinic);
		member.setUserId(userId);
		member.setRole(planTier == PlanTier.CONSULTORIO ? ClinicMemberRole.DIRECTOR : ClinicMemberRole.NUTRITIONIST);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		member.setInvitedBy(invitedByUserId);
		clinicMemberRepository.save(member);
		recordProvisioningAudit(subscription, userId, planTier);
		if (log.isInfoEnabled()) {
			log.info("Provisioned clinic access: userId={}, planTier={}, subscriptionId={}", userId, planTier,
					subscription.getId());
		}
	}

	private void syncAuth0RoleIfConfigured(final String userId, final PlanTier planTier) {
		if (!auth0RoleSyncClient.isConfigured()) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role sync skipped: Management API not configured, userId present={}",
						StringUtils.hasText(userId));
			}
			return;
		}
		auth0RoleSyncClient.syncPlanRole(userId, planTier);
	}

	private void recordProvisioningAudit(final Subscription subscription, final String userId,
			final PlanTier planTier) {
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.INVITATION_REDEEMED);
		event.setActorUserId(userId);
		event.setDetails("planTier=" + planTier);
		auditEventRepository.save(event);
	}

}
