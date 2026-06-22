package com.nutriconsultas.subscription.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicInvitation;
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

	/**
	 * Auth0 role assigned to clinic nutritionists invited by a director (inherits clinic
	 * subscription for entitlements).
	 */
	private static final PlanTier CLINIC_NUTRITIONIST_AUTH0_ROLE = PlanTier.PROFESIONAL;

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

	/**
	 * Links a redeemed clinic invitation to an existing consultorio (no separate payment
	 * or subscription).
	 */
	@Transactional
	public void provisionClinicInvitationMember(final ClinicInvitation invitation, final String userId) {
		if (invitation == null || invitation.getClinic() == null) {
			throw new IllegalArgumentException("invitation and clinic are required");
		}
		if (!StringUtils.hasText(userId)) {
			throw new IllegalArgumentException("userId is required");
		}
		final Clinic clinic = invitation.getClinic();
		final Subscription subscription = clinic.getSubscription();
		if (subscription == null || !grantsClinicAccess(subscription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.GONE, "La suscripción del consultorio ya no está activa");
		}
		final Optional<ClinicMember> existingMember = clinicMemberRepository
			.findByUserIdWithClinicAndSubscription(userId);
		if (existingMember.isPresent()) {
			reactivateOrVerifyExistingClinicMember(existingMember.get(), clinic, userId, invitation, subscription);
			return;
		}
		final ClinicMember member = new ClinicMember();
		member.setClinic(clinic);
		member.setUserId(userId);
		member.setRole(ClinicMemberRole.NUTRITIONIST);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		member.setInvitedBy(invitation.getInvitedByUserId());
		clinicMemberRepository.save(member);
		syncAuth0RoleIfConfigured(userId, CLINIC_NUTRITIONIST_AUTH0_ROLE);
		recordClinicInvitationAudit(subscription, userId, invitation.getId());
		if (log.isInfoEnabled()) {
			log.info("Provisioned clinic invitation member: userId={}, clinicId={}, invitationId={}", userId,
					clinic.getId(), invitation.getId());
		}
	}

	private void reactivateOrVerifyExistingClinicMember(final ClinicMember member, final Clinic clinic,
			final String userId, final ClinicInvitation invitation, final Subscription subscription) {
		if (!member.getClinic().getId().equals(clinic.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Ya perteneces a otro consultorio. Contacta al administrador.");
		}
		if (member.getMembershipStatus() == MembershipStatus.SUSPENDED) {
			member.setMembershipStatus(MembershipStatus.ACTIVE);
			clinicMemberRepository.save(member);
		}
		syncAuth0RoleIfConfigured(userId, CLINIC_NUTRITIONIST_AUTH0_ROLE);
		recordClinicInvitationAudit(subscription, userId, invitation.getId());
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

	private void relinkClinicIfRevoked(final Clinic clinic, final ClinicMember member, final PlanTier planTier,
			final Subscription subscription, final String userId) {
		final Subscription existingSubscription = clinic.getSubscription();
		if (existingSubscription != null && existingSubscription.getId().equals(subscription.getId())) {
			return;
		}
		if (existingSubscription != null && grantsClinicAccess(existingSubscription.getStatus())) {
			return;
		}
		clinic.setSubscription(subscription);
		clinicRepository.save(clinic);
		if (member != null) {
			member.setMembershipStatus(MembershipStatus.ACTIVE);
			clinicMemberRepository.save(member);
		}
		recordProvisioningAudit(subscription, userId, planTier);
		if (log.isInfoEnabled()) {
			log.info("Re-linked clinic to new subscription: userId={}, clinicId={}, subscriptionId={}, planTier={}",
					userId, clinic.getId(), subscription.getId(), planTier);
		}
	}

	private static boolean grantsClinicAccess(final SubscriptionStatus status) {
		return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE
				|| status == SubscriptionStatus.GRACE;
	}

	private void provisionClinicAccess(final String userId, final PlanTier planTier, final Subscription subscription,
			final String invitedByUserId) {
		final Optional<ClinicMember> memberOpt = clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId);
		if (memberOpt.isPresent()) {
			relinkClinicIfRevoked(memberOpt.get().getClinic(), memberOpt.get(), planTier, subscription, userId);
			return;
		}
		final Optional<Clinic> directorClinic = clinicRepository.findByDirectorUserIdWithSubscription(userId);
		if (directorClinic.isPresent()) {
			relinkClinicIfRevoked(directorClinic.get(), null, planTier, subscription, userId);
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
		try {
			auth0RoleSyncClient.syncPlanRole(userId, planTier);
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role sync failed; DB subscription access was provisioned: userId={}, planTier={}",
						userId, planTier);
			}
			if (log.isDebugEnabled()) {
				log.debug("Auth0 role sync failure", ex);
			}
		}
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

	private void recordClinicInvitationAudit(final Subscription subscription, final String userId,
			final Long invitationId) {
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.INVITATION_REDEEMED);
		event.setActorUserId(userId);
		event.setDetails("clinicInvitationId=" + invitationId);
		auditEventRepository.save(event);
	}

}
