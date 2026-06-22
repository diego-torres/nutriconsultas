package com.nutriconsultas.subscription.clinic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.PlanEntitlements;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClinicServiceImpl implements ClinicService {

	private final ClinicRepository clinicRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final ClinicInvitationRepository clinicInvitationRepository;

	private final ClinicMemberLabelResolver clinicMemberLabelResolver;

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	private final SubscriptionAccessService subscriptionAccessService;

	private final SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	private final Auth0RoleSyncClient auth0RoleSyncClient;

	public ClinicServiceImpl(final ClinicRepository clinicRepository,
			final ClinicMemberRepository clinicMemberRepository,
			final ClinicInvitationRepository clinicInvitationRepository,
			final ClinicMemberLabelResolver clinicMemberLabelResolver,
			final SubscriptionEntitlementService subscriptionEntitlementService,
			final SubscriptionAccessService subscriptionAccessService,
			final SubscriptionAuditEventRepository subscriptionAuditEventRepository,
			final Auth0RoleSyncClient auth0RoleSyncClient) {
		this.clinicRepository = clinicRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.clinicInvitationRepository = clinicInvitationRepository;
		this.clinicMemberLabelResolver = clinicMemberLabelResolver;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
		this.subscriptionAccessService = subscriptionAccessService;
		this.subscriptionAuditEventRepository = subscriptionAuditEventRepository;
		this.auth0RoleSyncClient = auth0RoleSyncClient;
	}

	@Override
	@Transactional(readOnly = true)
	public ClinicRosterOverview getDirectorRoster(@NonNull final String directorUserId) {
		final Clinic clinic = requireDirectorClinic(directorUserId);
		return buildRosterOverview(clinic, directorUserId);
	}

	@Override
	@Transactional
	public void suspendMember(@NonNull final String directorUserId, @NonNull final Long memberId) {
		final Clinic clinic = requireDirectorClinic(directorUserId);
		final ClinicMember member = requireMutableMember(clinic, directorUserId, memberId);
		if (member.getMembershipStatus() == MembershipStatus.SUSPENDED) {
			return;
		}
		member.setMembershipStatus(MembershipStatus.SUSPENDED);
		clinicMemberRepository.save(member);
		final String auth0Detail = revokeAuth0RolesIfConfigured(member.getUserId());
		recordDirectorAction(directorUserId, clinic.getSubscription(), "clinic.member.suspend,memberId=" + memberId
				+ ",targetUserId=" + member.getUserId() + "," + auth0Detail);
		if (log.isInfoEnabled()) {
			log.info("Suspended clinic member: clinicId={}, memberId={}, directorUserId present={}", clinic.getId(),
					memberId, StringUtils.hasText(directorUserId));
		}
	}

	@Override
	@Transactional
	public void reactivateMember(@NonNull final String directorUserId, @NonNull final Long memberId) {
		final Clinic clinic = requireDirectorClinic(directorUserId);
		final ClinicMember member = requireMutableMember(clinic, directorUserId, memberId);
		if (member.getMembershipStatus() == MembershipStatus.ACTIVE) {
			return;
		}
		assertSeatAvailableForReactivation(clinic, directorUserId);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		clinicMemberRepository.save(member);
		recordDirectorAction(directorUserId, resolveSubscription(clinic, directorUserId),
				"clinic.member.reactivate,memberId=" + memberId + ",targetUserId=" + member.getUserId());
		if (log.isInfoEnabled()) {
			log.info("Reactivated clinic member: clinicId={}, memberId={}, directorUserId present={}", clinic.getId(),
					memberId, StringUtils.hasText(directorUserId));
		}
	}

	private Clinic requireDirectorClinic(final String directorUserId) {
		if (!subscriptionEntitlementService.hasEntitlement(directorUserId, Entitlement.USER_ADMINISTRATION)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"No tienes permiso para administrar el consultorio");
		}
		return clinicRepository.findByDirectorUserIdWithSubscription(directorUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado"));
	}

	private ClinicMember requireMutableMember(final Clinic clinic, final String directorUserId, final Long memberId) {
		final ClinicMember member = clinicMemberRepository.findById(memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado"));
		if (!member.getClinic().getId().equals(clinic.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"No puedes administrar miembros de otro consultorio");
		}
		if (member.getRole() == ClinicMemberRole.DIRECTOR) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes suspender al director del consultorio");
		}
		if (member.getUserId().equals(directorUserId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes suspender tu propia cuenta");
		}
		return member;
	}

	private void assertSeatAvailableForReactivation(final Clinic clinic, final String directorUserId) {
		try {
			subscriptionEntitlementService.assertCanInviteNutritionist(directorUserId);
		}
		catch (SubscriptionLimitExceededException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"No hay plazas disponibles para reactivar a este nutriólogo");
		}
	}

	private ClinicRosterOverview buildRosterOverview(final Clinic clinic, final String directorUserId) {
		final Subscription subscription = resolveSubscription(clinic, directorUserId);
		final PlanEntitlements entitlements = PlanEntitlements.forTier(subscription.getPlanTier());
		final long activeSeatCount = clinicMemberRepository.countByClinicIdAndMembershipStatus(clinic.getId(),
				MembershipStatus.ACTIVE);
		final long pendingInviteCount = clinicInvitationRepository.countByClinicIdAndStatus(clinic.getId(),
				InvitationStatus.PENDING);
		final List<ClinicMemberView> members = new ArrayList<>();
		for (final ClinicMember member : clinicMemberRepository.findByClinicIdOrderByCreatedAtAsc(clinic.getId())) {
			members.add(toMemberView(member, directorUserId));
		}
		return new ClinicRosterOverview(clinic.getId(), clinic.getName(), subscription.getPlanTier(),
				subscription.getStatus(), entitlements.getMaxNutritionists(), activeSeatCount, pendingInviteCount,
				members);
	}

	private ClinicMemberView toMemberView(final ClinicMember member, final String directorUserId) {
		final String displayLabel = clinicMemberLabelResolver.resolveLabel(member.getUserId());
		return new ClinicMemberView(member.getId(), member.getUserId(), displayLabel, member.getRole(),
				member.getMembershipStatus(), member.getCreatedAt(), member.getUserId().equals(directorUserId));
	}

	private Subscription resolveSubscription(final Clinic clinic, final String directorUserId) {
		return subscriptionAccessService.findGrantingSubscriptionForUser(directorUserId)
			.orElse(clinic.getSubscription());
	}

	private void recordDirectorAction(final String directorUserId, final Subscription subscription,
			final String details) {
		if (!StringUtils.hasText(directorUserId)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.CLINIC_DIRECTOR_ACTION);
		event.setActorUserId(directorUserId);
		event.setDetails(details);
		subscriptionAuditEventRepository.save(event);
	}

	private String revokeAuth0RolesIfConfigured(final String targetUserId) {
		if (!StringUtils.hasText(targetUserId) || !auth0RoleSyncClient.isConfigured()) {
			return "auth0Revoked=skipped";
		}
		try {
			auth0RoleSyncClient.revokePlanRoles(targetUserId);
			return "auth0Revoked=true";
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 role revoke failed after clinic member suspend: targetUserId present={}",
						StringUtils.hasText(targetUserId));
			}
			if (log.isDebugEnabled()) {
				log.debug("Auth0 role revoke failure", ex);
			}
			return "auth0Revoked=false";
		}
	}

}
