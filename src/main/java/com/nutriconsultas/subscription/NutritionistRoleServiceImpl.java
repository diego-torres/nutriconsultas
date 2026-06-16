package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistRoleServiceImpl implements NutritionistRoleService {

	private final PlatformAdminService platformAdminService;

	private final ClinicRepository clinicRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final Auth0RoleSyncClient auth0RoleSyncClient;

	private final SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	public NutritionistRoleServiceImpl(final PlatformAdminService platformAdminService,
			final ClinicRepository clinicRepository, final ClinicMemberRepository clinicMemberRepository,
			final SubscriptionRepository subscriptionRepository, final Auth0RoleSyncClient auth0RoleSyncClient,
			final SubscriptionAuditEventRepository subscriptionAuditEventRepository) {
		this.platformAdminService = platformAdminService;
		this.clinicRepository = clinicRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.subscriptionRepository = subscriptionRepository;
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
		final PlanTier previousTier = subscription.getPlanTier();
		subscription.setPlanTier(planTier);
		subscriptionRepository.save(subscription);
		auth0RoleSyncClient.syncPlanRole(targetUserId, planTier);
		recordRoleAssignment(platformAdminService.resolveActorUserId(adminPrincipal), targetUserId, previousTier,
				planTier);
		if (log.isInfoEnabled()) {
			log.info("Assigned plan tier: targetUserId={}, previousTier={}, newTier={}", targetUserId, previousTier,
					planTier);
		}
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

	private void recordRoleAssignment(final String actorUserId, final String targetUserId, final PlanTier previousTier,
			final PlanTier newTier) {
		if (!StringUtils.hasText(actorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setEventType(SubscriptionAuditEventType.ROLE_ASSIGNED);
		event.setActorUserId(actorUserId);
		event.setDetails("targetUserId=" + targetUserId + ",previousTier=" + previousTier + ",newTier=" + newTier);
		subscriptionAuditEventRepository.save(event);
	}

}
