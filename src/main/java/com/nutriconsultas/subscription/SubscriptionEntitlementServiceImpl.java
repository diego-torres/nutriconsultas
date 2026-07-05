package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubscriptionEntitlementServiceImpl implements SubscriptionEntitlementService {

	private final ClinicMemberRepository clinicMemberRepository;

	private final ClinicRepository clinicRepository;

	private final ClinicInvitationRepository clinicInvitationRepository;

	private final PacienteRepository pacienteRepository;

	private final SubscriptionProperties subscriptionProperties;

	private final SubscriptionAccessService subscriptionAccessService;

	public SubscriptionEntitlementServiceImpl(final ClinicMemberRepository clinicMemberRepository,
			final ClinicRepository clinicRepository, final ClinicInvitationRepository clinicInvitationRepository,
			final PacienteRepository pacienteRepository, final SubscriptionProperties subscriptionProperties,
			final SubscriptionAccessService subscriptionAccessService) {
		this.clinicMemberRepository = clinicMemberRepository;
		this.clinicRepository = clinicRepository;
		this.clinicInvitationRepository = clinicInvitationRepository;
		this.pacienteRepository = pacienteRepository;
		this.subscriptionProperties = subscriptionProperties;
		this.subscriptionAccessService = subscriptionAccessService;
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

	@Override
	@Transactional(readOnly = true)
	public void assertCanCreatePatient(@NonNull final String userId) {
		if (!hasEntitlement(userId, Entitlement.CREATE_PATIENT)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_CREATE_PATIENT_DENIED);
		}
		final UserAccessContext access = resolveAccess(userId).orElseThrow(
				() -> new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_CREATE_PATIENT_DENIED));
		final PlanEntitlements planEntitlements = PlanEntitlements.forTier(access.subscription().getPlanTier());
		final Integer maxPatients = planEntitlements.getMaxPatients();
		if (maxPatients == null) {
			return;
		}
		final long patientCount = countPatientsInClinicScope(access.clinicId());
		if (patientCount >= maxPatients) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_PATIENT_LIMIT, maxPatients);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void assertCanInviteNutritionist(@NonNull final String directorUserId) {
		if (!hasEntitlement(directorUserId, Entitlement.USER_ADMINISTRATION)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_NUTRITIONIST_INVITE_DENIED);
		}
		final Clinic clinic = clinicRepository.findByDirectorUserIdWithSubscription(directorUserId)
			.orElseThrow(() -> new SubscriptionLimitExceededException(
					SubscriptionErrorResponses.KEY_NUTRITIONIST_INVITE_DENIED));
		final Subscription subscription = subscriptionAccessService.findGrantingSubscriptionForUser(directorUserId)
			.orElse(clinic.getSubscription());
		final PlanEntitlements planEntitlements = PlanEntitlements.forTier(subscription.getPlanTier());
		final int maxNutritionists = planEntitlements.getMaxNutritionists();
		final long activeMembers = clinicMemberRepository.countByClinicIdAndMembershipStatus(clinic.getId(),
				MembershipStatus.ACTIVE);
		final long pendingInvites = clinicInvitationRepository.countByClinicIdAndStatus(clinic.getId(),
				InvitationStatus.PENDING);
		if (activeMembers + pendingInvites >= maxNutritionists) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_NUTRITIONIST_LIMIT,
					maxNutritionists);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void assertCanExportPdf(@NonNull final String userId) {
		if (!hasEntitlement(userId, Entitlement.PDF_EXPORT)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_PDF_EXPORT_DENIED);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void assertCanAccessAdvancedReports(@NonNull final String userId) {
		if (!hasEntitlement(userId, Entitlement.REPORTS_ADVANCED)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_REPORTS_ADVANCED_DENIED);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void assertCanAccessFullReports(@NonNull final String userId) {
		if (!hasEntitlement(userId, Entitlement.REPORTS_FULL)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_REPORTS_FULL_DENIED);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void assertCanUseAiAssistant(@NonNull final String userId) {
		if (!hasEntitlement(userId, Entitlement.AI_ASSISTANT)) {
			throw new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED);
		}
	}

	private long countPatientsInClinicScope(final Long clinicId) {
		final List<String> memberUserIds = clinicMemberRepository.findUserIdsByClinicIdAndMembershipStatus(clinicId,
				MembershipStatus.ACTIVE);
		if (memberUserIds.isEmpty()) {
			return 0L;
		}
		return pacienteRepository.countByUserIdIn(memberUserIds);
	}

	private Optional<UserAccessContext> resolveAccess(final String userId) {
		final Optional<ClinicMember> memberOpt = clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId);
		if (memberOpt.isEmpty()) {
			return clinicRepository.findByDirectorUserIdWithSubscription(userId).flatMap(clinic -> {
				final Subscription subscription = subscriptionAccessService.findGrantingSubscriptionForUser(userId)
					.orElse(clinic.getSubscription());
				return Optional.of(new UserAccessContext(subscription, clinic.getId(), true));
			});
		}
		final ClinicMember member = memberOpt.get();
		if (member.getMembershipStatus() == MembershipStatus.SUSPENDED) {
			logSuspendedMember(userId);
			return Optional.empty();
		}
		final boolean director = member.getRole() == ClinicMemberRole.DIRECTOR;
		final Subscription subscription = subscriptionAccessService.findGrantingSubscriptionForUser(userId)
			.orElse(member.getClinic().getSubscription());
		return Optional.of(new UserAccessContext(subscription, member.getClinic().getId(), director));
	}

	private void logSuspendedMember(final String userId) {
		if (log.isDebugEnabled()) {
			log.debug("Clinic member suspended; no subscription entitlements for userId={}", userId);
		}
	}

	private record UserAccessContext(Subscription subscription, Long clinicId, boolean director) {
	}

	private static boolean grantsEntitlements(final SubscriptionStatus status) {
		return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE
				|| status == SubscriptionStatus.GRACE;
	}

}
