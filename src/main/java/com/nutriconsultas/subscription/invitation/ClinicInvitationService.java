package com.nutriconsultas.subscription.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicInvitation;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

/**
 * Clinic director invitations (no payment). Directors invite nutritionists to join their
 * consultorio; access inherits the clinic subscription.
 */
@Service
@Slf4j
public class ClinicInvitationService {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	private final ClinicRepository clinicRepository;

	private final ClinicInvitationRepository clinicInvitationRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final NutritionistInvitationRepository nutritionistInvitationRepository;

	private final NutritionistInvitationProperties invitationProperties;

	private final InvitationEmailSender invitationEmailSender;

	private final SubscriptionProvisioningService provisioningService;

	private final SubscriptionAuditEventRepository auditEventRepository;

	public ClinicInvitationService(final SubscriptionEntitlementService subscriptionEntitlementService,
			final ClinicRepository clinicRepository, final ClinicInvitationRepository clinicInvitationRepository,
			final ClinicMemberRepository clinicMemberRepository,
			final NutritionistInvitationRepository nutritionistInvitationRepository,
			final NutritionistInvitationProperties invitationProperties,
			final InvitationEmailSender invitationEmailSender,
			final SubscriptionProvisioningService provisioningService,
			final SubscriptionAuditEventRepository auditEventRepository) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
		this.clinicRepository = clinicRepository;
		this.clinicInvitationRepository = clinicInvitationRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.nutritionistInvitationRepository = nutritionistInvitationRepository;
		this.invitationProperties = invitationProperties;
		this.invitationEmailSender = invitationEmailSender;
		this.provisioningService = provisioningService;
		this.auditEventRepository = auditEventRepository;
	}

	/**
	 * Validates the director may invite another nutritionist before persisting a
	 * {@link ClinicInvitation}.
	 * @throws SubscriptionLimitExceededException when the plan seat cap is reached
	 */
	public void assertCanInviteNutritionist(@NonNull final String directorUserId) {
		subscriptionEntitlementService.assertCanInviteNutritionist(directorUserId);
	}

	@Transactional
	public CreatedClinicInvitation createInvitation(final OidcUser directorPrincipal, final String email) {
		final String directorUserId = requireDirectorUserId(directorPrincipal);
		assertCanInviteNutritionist(directorUserId);
		final Clinic clinic = requireDirectorClinic(directorUserId);
		final String normalizedEmail = normalizeEmail(email);
		clinicInvitationRepository.findByEmailIgnoreCaseAndStatus(normalizedEmail, InvitationStatus.PENDING)
			.ifPresent(existing -> {
				throw new PendingClinicInvitationException(existing.getId());
			});
		nutritionistInvitationRepository.findByEmailIgnoreCaseAndStatus(normalizedEmail, InvitationStatus.PENDING)
			.ifPresent(existing -> {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Este correo ya tiene una invitación pendiente de la plataforma");
			});
		rejectIfActiveNutritionist(normalizedEmail);
		final String rawToken = InvitationTokenHasher.generateToken();
		final ClinicInvitation invitation = new ClinicInvitation();
		invitation.setClinic(clinic);
		invitation.setEmail(normalizedEmail);
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		invitation.setInvitedByUserId(directorUserId);
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS));
		final ClinicInvitation saved = clinicInvitationRepository.save(invitation);
		final String inviteUrl = invitationProperties.buildClinicRedeemUrl(rawToken);
		invitationEmailSender.sendClinicInvitation(normalizedEmail, clinic.getName(), inviteUrl);
		recordDirectorInvitationAudit(directorUserId, clinic, saved.getId(), normalizedEmail);
		if (log.isInfoEnabled()) {
			log.info("Created clinic invitation: invitationId={}, clinicId={}", saved.getId(), clinic.getId());
		}
		return new CreatedClinicInvitation(saved.getId(), inviteUrl);
	}

	@Transactional
	public void cancelInvitation(final OidcUser directorPrincipal, final Long invitationId) {
		final String directorUserId = requireDirectorUserId(directorPrincipal);
		final Clinic clinic = requireDirectorClinic(directorUserId);
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final ClinicInvitation invitation = clinicInvitationRepository.findByIdAndClinicId(invitationId, clinic.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no encontrada"));
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar invitaciones pendientes");
		}
		invitation.setStatus(InvitationStatus.CANCELLED);
		clinicInvitationRepository.save(invitation);
		recordDirectorCancelAudit(directorUserId, clinic, invitation.getId());
		if (log.isInfoEnabled()) {
			log.info("Cancelled clinic invitation: invitationId={}, clinicId={}", invitation.getId(), clinic.getId());
		}
	}

	@Transactional
	public String regenerateInvitationLink(final OidcUser directorPrincipal, final Long invitationId) {
		final String directorUserId = requireDirectorUserId(directorPrincipal);
		final Clinic clinic = requireDirectorClinic(directorUserId);
		if (invitationId == null) {
			throw new IllegalArgumentException("invitationId is required");
		}
		final ClinicInvitation invitation = clinicInvitationRepository.findByIdAndClinicId(invitationId, clinic.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no encontrada"));
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se puede obtener el enlace de invitaciones pendientes");
		}
		final String rawToken = InvitationTokenHasher.generateToken();
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		if (Instant.now().isAfter(invitation.getExpiresAt())) {
			invitation.setExpiresAt(Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS));
		}
		clinicInvitationRepository.save(invitation);
		final String inviteUrl = invitationProperties.buildClinicRedeemUrl(rawToken);
		if (log.isInfoEnabled()) {
			log.info("Regenerated clinic invitation link: invitationId={}, clinicId={}", invitation.getId(),
					clinic.getId());
		}
		return inviteUrl;
	}

	@Transactional
	public void redeemInvitation(final OidcUser principal, final String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			throw new IllegalArgumentException("token is required");
		}
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		final ClinicInvitation invitation = findValidInvitation(rawToken);
		verifyEmailMatches(principal, invitation);
		markRedeemed(invitation, principal.getSubject());
		provisioningService.provisionClinicInvitationMember(invitation, principal.getSubject());
	}

	ClinicInvitation findValidInvitation(final String rawToken) {
		final String tokenHash = InvitationTokenHasher.hashToken(rawToken);
		final ClinicInvitation invitation = clinicInvitationRepository.findByTokenHash(tokenHash)
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
			clinicInvitationRepository.save(invitation);
			throw new ResponseStatusException(HttpStatus.GONE, "Invitation has expired");
		}
		return invitation;
	}

	private void verifyEmailMatches(final OidcUser principal, final ClinicInvitation invitation) {
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

	private void markRedeemed(final ClinicInvitation invitation, final String userId) {
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setRedeemedAt(Instant.now());
		invitation.setRedeemedByUserId(userId);
		clinicInvitationRepository.save(invitation);
	}

	private void rejectIfActiveNutritionist(final String normalizedEmail) {
		nutritionistInvitationRepository
			.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(normalizedEmail, InvitationStatus.REDEEMED)
			.filter(this::hasActivePlatformAccess)
			.ifPresent(redeemed -> {
				throw new ActiveNutritionistUserException(redeemed.getId());
			});
		rejectIfActiveClinicMember(normalizedEmail);
	}

	private void rejectIfActiveClinicMember(final String normalizedEmail) {
		final Optional<ClinicInvitation> redeemedInvitation = clinicInvitationRepository
			.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(normalizedEmail, InvitationStatus.REDEEMED);
		if (redeemedInvitation.isEmpty() || !StringUtils.hasText(redeemedInvitation.get().getRedeemedByUserId())) {
			return;
		}
		final Optional<ClinicMember> member = clinicMemberRepository
			.findByUserIdWithClinicAndSubscription(redeemedInvitation.get().getRedeemedByUserId());
		if (member.isPresent()
				&& ClinicInvitationAccessRules.blocksNewInvitation(redeemedInvitation.get(), member.get())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Este correo ya tiene acceso activo en un consultorio");
		}
	}

	private boolean hasActivePlatformAccess(final NutritionistInvitation redeemedInvitation) {
		final Subscription subscription = redeemedInvitation.getSubscription();
		return subscription != null && NutritionistInvitationAccessRules.blocksNewInvitation(subscription.getStatus());
	}

	private Clinic requireDirectorClinic(final String directorUserId) {
		return clinicRepository.findByDirectorUserIdWithSubscription(directorUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado"));
	}

	private String requireDirectorUserId(final OidcUser directorPrincipal) {
		if (directorPrincipal == null || !StringUtils.hasText(directorPrincipal.getSubject())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return directorPrincipal.getSubject();
	}

	private void recordDirectorInvitationAudit(final String directorUserId, final Clinic clinic,
			final Long invitationId, final String email) {
		recordDirectorAction(directorUserId, clinic,
				"clinic.invitation.create,invitationId=" + invitationId + ",email=" + email);
	}

	private void recordDirectorCancelAudit(final String directorUserId, final Clinic clinic, final Long invitationId) {
		recordDirectorAction(directorUserId, clinic, "clinic.invitation.cancel,invitationId=" + invitationId);
	}

	private void recordDirectorAction(final String directorUserId, final Clinic clinic, final String details) {
		if (!StringUtils.hasText(directorUserId) || clinic.getSubscription() == null) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(clinic.getSubscription());
		event.setEventType(SubscriptionAuditEventType.CLINIC_DIRECTOR_ACTION);
		event.setActorUserId(directorUserId);
		event.setDetails(details);
		auditEventRepository.save(event);
	}

	private static String normalizeEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
