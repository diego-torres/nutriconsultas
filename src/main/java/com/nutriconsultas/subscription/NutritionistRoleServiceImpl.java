package com.nutriconsultas.subscription;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistRoleServiceImpl implements NutritionistRoleService {

	private static final Set<SubscriptionStatus> TIER_CHANGEABLE_STATUSES = EnumSet.of(SubscriptionStatus.TRIAL,
			SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE);

	private final PlatformAdminService platformAdminService;

	private final ClinicRepository clinicRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final NutritionistInvitationRepository invitationRepository;

	private final PacienteRepository pacienteRepository;

	private final Auth0RoleSyncClient auth0RoleSyncClient;

	private final SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	public NutritionistRoleServiceImpl(final PlatformAdminService platformAdminService,
			final ClinicRepository clinicRepository, final ClinicMemberRepository clinicMemberRepository,
			final SubscriptionRepository subscriptionRepository,
			final NutritionistInvitationRepository invitationRepository, final PacienteRepository pacienteRepository,
			final Auth0RoleSyncClient auth0RoleSyncClient,
			final SubscriptionAuditEventRepository subscriptionAuditEventRepository) {
		this.platformAdminService = platformAdminService;
		this.clinicRepository = clinicRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.invitationRepository = invitationRepository;
		this.pacienteRepository = pacienteRepository;
		this.auth0RoleSyncClient = auth0RoleSyncClient;
		this.subscriptionAuditEventRepository = subscriptionAuditEventRepository;
	}

	@Override
	@Transactional
	public void assignRole(final OidcUser adminPrincipal, final String targetUserId, final PlanTier planTier) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		if (!StringUtils.hasText(targetUserId)) {
			throw new IllegalArgumentException("targetUserId is required");
		}
		if (planTier == null) {
			throw new IllegalArgumentException("planTier is required");
		}
		final Subscription subscription = resolveSubscription(targetUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found for user"));
		if (!TIER_CHANGEABLE_STATUSES.contains(subscription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se puede cambiar el plan de suscripciones activas, en prueba o en periodo de gracia");
		}
		final Clinic clinic = clinicRepository.findBySubscriptionId(subscription.getId()).orElse(null);
		assertUsageFitsPlanTier(clinic, planTier);
		applyPlanTierChange(platformAdminService.resolveActorUserId(adminPrincipal), subscription, planTier,
				SubscriptionAuditEventType.ROLE_ASSIGNED, targetUserId,
				invitationRepository.findBySubscriptionId(subscription.getId()).orElse(null));
	}

	@Override
	@Transactional
	public PlanTierChangeResult changeSubscriptionPlanTier(final OidcUser adminPrincipal, final Long subscriptionId,
			final PlanTier newTier) {
		platformAdminService.requirePlatformAdmin(adminPrincipal);
		if (subscriptionId == null) {
			throw new IllegalArgumentException("subscriptionId is required");
		}
		if (newTier == null) {
			throw new IllegalArgumentException("planTier is required");
		}
		final Subscription subscription = subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
		if (!TIER_CHANGEABLE_STATUSES.contains(subscription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se puede cambiar el plan de suscripciones activas, en prueba o en periodo de gracia");
		}
		final PlanTier previousTier = subscription.getPlanTier();
		if (previousTier == newTier) {
			return new PlanTierChangeResult(previousTier, newTier, true);
		}
		final Clinic clinic = clinicRepository.findBySubscriptionId(subscriptionId).orElse(null);
		assertUsageFitsPlanTier(clinic, newTier);
		final String targetUserId = resolveTargetUserId(subscriptionId, clinic);
		final boolean auth0SyncSucceeded = applyPlanTierChange(platformAdminService.resolveActorUserId(adminPrincipal),
				subscription, newTier, SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION, targetUserId,
				invitationRepository.findBySubscriptionId(subscriptionId).orElse(null));
		return new PlanTierChangeResult(previousTier, newTier, auth0SyncSucceeded);
	}

	private boolean applyPlanTierChange(final String actorUserId, final Subscription subscription,
			final PlanTier newTier, final SubscriptionAuditEventType auditType, final String targetUserId,
			final NutritionistInvitation invitation) {
		final PlanTier previousTier = subscription.getPlanTier();
		subscription.setPlanTier(newTier);
		subscriptionRepository.save(subscription);
		if (invitation != null) {
			invitation.setPlanTier(newTier);
		}
		final boolean auth0SyncSucceeded = syncAuth0RoleIfConfigured(targetUserId, newTier);
		recordPlanTierChange(actorUserId, subscription, previousTier, newTier, auditType, targetUserId,
				auth0SyncSucceeded);
		if (log.isInfoEnabled()) {
			log.info("Changed plan tier: subscriptionId={}, previousTier={}, newTier={}, targetUserIdPresent={}",
					subscription.getId(), previousTier, newTier, StringUtils.hasText(targetUserId));
		}
		return auth0SyncSucceeded;
	}

	private void assertUsageFitsPlanTier(final Clinic clinic, final PlanTier newTier) {
		if (clinic == null) {
			return;
		}
		final PlanEntitlements newEntitlements = PlanEntitlements.forTier(newTier);
		final Integer maxPatients = newEntitlements.getMaxPatients();
		if (maxPatients != null) {
			final long patientCount = countPatientsInClinic(clinic.getId());
			if (patientCount > maxPatients) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"No se puede cambiar al plan " + newTier + ": el consultorio tiene " + patientCount
								+ " pacientes y el plan permite máximo " + maxPatients + ".");
			}
		}
		final int maxNutritionists = newEntitlements.getMaxNutritionists();
		final long activeMembers = clinicMemberRepository.countByClinicIdAndMembershipStatus(clinic.getId(),
				MembershipStatus.ACTIVE);
		if (activeMembers > maxNutritionists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"No se puede cambiar al plan " + newTier + ": el consultorio tiene " + activeMembers
							+ " nutriólogos activos y el plan permite máximo " + maxNutritionists + ".");
		}
	}

	private long countPatientsInClinic(final Long clinicId) {
		final List<String> memberUserIds = clinicMemberRepository.findUserIdsByClinicIdAndMembershipStatus(clinicId,
				MembershipStatus.ACTIVE);
		if (memberUserIds.isEmpty()) {
			return 0L;
		}
		return pacienteRepository.countByUserIdIn(memberUserIds);
	}

	private String resolveTargetUserId(final Long subscriptionId, final Clinic clinic) {
		final Optional<String> redeemedUserId = invitationRepository.findBySubscriptionId(subscriptionId)
			.map(NutritionistInvitation::getRedeemedByUserId)
			.filter(StringUtils::hasText);
		if (redeemedUserId.isPresent()) {
			return redeemedUserId.get();
		}
		if (clinic != null && StringUtils.hasText(clinic.getDirectorUserId())) {
			return clinic.getDirectorUserId();
		}
		return null;
	}

	private Optional<Subscription> resolveSubscription(final String userId) {
		final Optional<Subscription> directorSubscription = clinicRepository
			.findByDirectorUserIdWithSubscription(userId)
			.map(Clinic::getSubscription);
		if (directorSubscription.isPresent()) {
			return directorSubscription;
		}
		return clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId)
			.map(member -> member.getClinic().getSubscription());
	}

	private boolean syncAuth0RoleIfConfigured(final String targetUserId, final PlanTier planTier) {
		if (!StringUtils.hasText(targetUserId)) {
			return true;
		}
		if (!auth0RoleSyncClient.isConfigured()) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role sync skipped: Management API not configured, targetUserId present={}",
						StringUtils.hasText(targetUserId));
			}
			return true;
		}
		try {
			auth0RoleSyncClient.syncPlanRole(targetUserId, planTier);
			return true;
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role sync failed after plan tier change: targetUserId present={}, planTier={}",
						StringUtils.hasText(targetUserId), planTier);
			}
			if (log.isDebugEnabled()) {
				log.debug("Auth0 role sync failure", ex);
			}
			return false;
		}
	}

	private void recordPlanTierChange(final String actorUserId, final Subscription subscription,
			final PlanTier previousTier, final PlanTier newTier, final SubscriptionAuditEventType auditType,
			final String targetUserId, final boolean auth0SyncSucceeded) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(auditType);
		event.setActorUserId(actorUserId);
		if (auditType == SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION) {
			event.setDetails("action=plan.tier.change,subscriptionId=" + subscription.getId() + ",previousTier="
					+ previousTier + ",newTier=" + newTier + ",targetUserId=" + targetUserId + ",auth0Synced="
					+ auth0SyncSucceeded);
		}
		else {
			event.setDetails("targetUserId=" + targetUserId + ",previousTier=" + previousTier + ",newTier=" + newTier
					+ ",auth0Synced=" + auth0SyncSucceeded);
		}
		subscriptionAuditEventRepository.save(event);
	}

}
