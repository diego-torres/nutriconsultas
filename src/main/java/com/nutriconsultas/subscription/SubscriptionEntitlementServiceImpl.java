package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubscriptionEntitlementServiceImpl implements SubscriptionEntitlementService {

	private final ClinicMemberRepository clinicMemberRepository;

	private final ClinicRepository clinicRepository;

	private final SubscriptionProperties subscriptionProperties;

	public SubscriptionEntitlementServiceImpl(final ClinicMemberRepository clinicMemberRepository,
			final ClinicRepository clinicRepository, final SubscriptionProperties subscriptionProperties) {
		this.clinicMemberRepository = clinicMemberRepository;
		this.clinicRepository = clinicRepository;
		this.subscriptionProperties = subscriptionProperties;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PlanTier> getEffectivePlanTier(@NonNull final String userId) {
		return resolveAccess(userId).map(access -> access.subscription().getPlanTier());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasEntitlement(@NonNull final String userId, @NonNull final Entitlement entitlement) {
		if (!StringUtils.hasText(userId)) {
			return false;
		}
		final Optional<UserAccessContext> accessOpt = resolveAccess(userId);
		if (accessOpt.isEmpty()) {
			return false;
		}
		final UserAccessContext access = accessOpt.get();
		final Subscription subscription = access.subscription();
		if (!grantsEntitlements(subscription.getStatus())) {
			return false;
		}
		final PlanEntitlements planEntitlements = PlanEntitlements.forTier(subscription.getPlanTier());
		if (!planEntitlements.hasEntitlement(entitlement)) {
			return false;
		}
		if (entitlement == Entitlement.USER_ADMINISTRATION && !access.director()) {
			return false;
		}
		return subscription.getStatus() != SubscriptionStatus.GRACE
				|| !subscriptionProperties.getGraceDeniedEntitlements().contains(entitlement);
	}

	private Optional<UserAccessContext> resolveAccess(final String userId) {
		final Optional<ClinicMember> memberOpt = clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId);
		if (memberOpt.isEmpty()) {
			return clinicRepository.findByDirectorUserIdWithSubscription(userId)
				.map(clinic -> new UserAccessContext(clinic.getSubscription(), true));
		}
		final ClinicMember member = memberOpt.get();
		if (member.getMembershipStatus() == MembershipStatus.SUSPENDED) {
			logSuspendedMember(userId);
			return Optional.empty();
		}
		final boolean director = member.getRole() == ClinicMemberRole.DIRECTOR;
		return Optional.of(new UserAccessContext(member.getClinic().getSubscription(), director));
	}

	private void logSuspendedMember(final String userId) {
		if (log.isDebugEnabled()) {
			log.debug("Clinic member suspended; no subscription entitlements for userId={}", userId);
		}
	}

	private record UserAccessContext(Subscription subscription, boolean director) {
	}

	private static boolean grantsEntitlements(final SubscriptionStatus status) {
		return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE
				|| status == SubscriptionStatus.GRACE;
	}

}
