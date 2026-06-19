package com.nutriconsultas.subscription.lifecycle;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

@Service
public class SubscriptionAccessService {

	private final ClinicMemberRepository clinicMemberRepository;

	private final ClinicRepository clinicRepository;

	private final NutritionistInvitationRepository invitationRepository;

	public SubscriptionAccessService(final ClinicMemberRepository clinicMemberRepository,
			final ClinicRepository clinicRepository, final NutritionistInvitationRepository invitationRepository) {
		this.clinicMemberRepository = clinicMemberRepository;
		this.clinicRepository = clinicRepository;
		this.invitationRepository = invitationRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Subscription> findSubscriptionForUser(final String userId) {
		return findGrantingSubscriptionForUser(userId).or(() -> findLinkedSubscription(userId));
	}

	@Transactional(readOnly = true)
	public Optional<Subscription> findGrantingSubscriptionForUser(final String userId) {
		final Optional<Subscription> linked = findLinkedSubscription(userId);
		if (linked.isPresent() && grantsAdminAccess(linked.get())) {
			return linked;
		}
		return invitationRepository
			.findFirstByRedeemedByUserIdAndStatusOrderByRedeemedAtDesc(userId, InvitationStatus.REDEEMED)
			.map(invitation -> invitation.getSubscription())
			.filter(this::grantsAdminAccess);
	}

	@Transactional(readOnly = true)
	public boolean isAdminAccessBlocked(final String userId) {
		if (findGrantingSubscriptionForUser(userId).isPresent()) {
			return false;
		}
		return findLinkedSubscription(userId).map(this::isBlockedStatus).orElse(false);
	}

	@Transactional(readOnly = true)
	public Optional<SubscriptionBanner> resolveBanner(final String userId) {
		return findGrantingSubscriptionForUser(userId).flatMap(this::bannerForSubscription);
	}

	private Optional<Subscription> findLinkedSubscription(final String userId) {
		return clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId)
			.filter(member -> member.getMembershipStatus() == MembershipStatus.ACTIVE)
			.map(member -> member.getClinic().getSubscription())
			.or(() -> clinicRepository.findByDirectorUserIdWithSubscription(userId)
				.map(clinic -> clinic.getSubscription()));
	}

	private boolean grantsAdminAccess(final Subscription subscription) {
		return subscription != null && !isBlockedStatus(subscription);
	}

	private boolean isBlockedStatus(final Subscription subscription) {
		final SubscriptionStatus status = subscription.getStatus();
		return status == SubscriptionStatus.SUSPENDED || status == SubscriptionStatus.CANCELLED
				|| status == SubscriptionStatus.PENDING_PAYMENT;
	}

	private Optional<SubscriptionBanner> bannerForSubscription(final Subscription subscription) {
		final SubscriptionBanner banner = SubscriptionBanner.forSubscription(subscription);
		if (banner == null) {
			return Optional.empty();
		}
		if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getPeriodEnd() != null) {
			final long daysUntilExpiry = java.time.Duration
				.between(java.time.Instant.now(), subscription.getPeriodEnd())
				.toDays();
			if (daysUntilExpiry > 7) {
				return Optional.empty();
			}
		}
		return Optional.of(banner);
	}

}
