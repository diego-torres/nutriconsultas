package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientMobileInvitationServiceImpl implements PatientMobileInvitationService {

	private final PacienteRepository pacienteRepository;

	private final PatientInvitationRepository patientInvitationRepository;

	private final PatientInvitationTokenService patientInvitationTokenService;

	private final PatientInvitationProperties invitationProperties;

	private final PatientInvitationEmailSender patientInvitationEmailSender;

	private final PatientInvitationRevokeService patientInvitationRevokeService;

	public PatientMobileInvitationServiceImpl(final PacienteRepository pacienteRepository,
			final PatientInvitationRepository patientInvitationRepository,
			final PatientInvitationTokenService patientInvitationTokenService,
			final PatientInvitationProperties invitationProperties,
			final PatientInvitationEmailSender patientInvitationEmailSender,
			final PatientInvitationRevokeService patientInvitationRevokeService) {
		this.pacienteRepository = pacienteRepository;
		this.patientInvitationRepository = patientInvitationRepository;
		this.patientInvitationTokenService = patientInvitationTokenService;
		this.invitationProperties = invitationProperties;
		this.patientInvitationEmailSender = patientInvitationEmailSender;
		this.patientInvitationRevokeService = patientInvitationRevokeService;
	}

	@Override
	@Transactional(readOnly = true)
	public PatientMobileInvitationStatus getStatus(final Long pacienteId, final String nutritionistUserId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		return PatientMobileInvitationUiSupport.resolve(paciente, findActivePendingInvitation(pacienteId).orElse(null));
	}

	@Override
	@Transactional
	public IssuedPatientMobileInvitationResult sendInvitation(final Long pacienteId, final String nutritionistUserId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		final PatientMobileInvitationStatus current = PatientMobileInvitationUiSupport.resolve(paciente,
				findActivePendingInvitation(pacienteId).orElse(null));
		if (!current.canSend() && !current.canResend()) {
			throw new PatientMobileInvitationNotAllowedException(current.stateCode());
		}
		final String recipientEmail = requireRecipientEmail(paciente);
		revokeExpiredPendingInvitations(pacienteId);
		findActivePendingInvitation(pacienteId)
			.ifPresent(pending -> patientInvitationRevokeService.revoke(pending.getId(), nutritionistUserId));

		ensureInviteMetadata(paciente);
		if (paciente.getStatus() == PacienteStatus.ACTIVE) {
			paciente.setStatus(PacienteStatus.INVITED);
		}
		pacienteRepository.save(paciente);

		final PatientInvitationTokenBundle tokenBundle = patientInvitationTokenService.generate();
		final Instant expiresAt = Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setTokenHash(tokenBundle.tokenHash());
		invitation.setHumanCode(tokenBundle.humanCode());
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(nutritionistUserId);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(expiresAt);
		invitation.setMaxUses(1);
		final PatientInvitation savedInvitation = patientInvitationRepository.save(invitation);

		final String inviteUrl = invitationProperties.buildInviteUrl(tokenBundle.urlToken());
		patientInvitationEmailSender.sendPatientInvitation(recipientEmail, tokenBundle.humanCode(), inviteUrl);

		if (log.isInfoEnabled()) {
			log.info("Issued patient mobile invitation from web: invitationId={}, pacienteId={}",
					savedInvitation.getId(), paciente.getId());
		}

		return new IssuedPatientMobileInvitationResult(savedInvitation.getId(), paciente.getId(),
				tokenBundle.humanCode(), expiresAt, LogRedaction.redactEmail(recipientEmail));
	}

	@Override
	@Transactional
	public PatientInvitationRevokeResult revokePendingInvitation(final Long pacienteId,
			final String nutritionistUserId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		final PatientInvitation pending = findActivePendingInvitation(pacienteId)
			.orElseThrow(() -> new PatientMobileInvitationNotAllowedException("NO_PENDING_INVITATION"));
		final PatientMobileInvitationStatus current = PatientMobileInvitationUiSupport.resolve(paciente, pending);
		if (!current.canRevoke()) {
			throw new PatientMobileInvitationNotAllowedException(current.stateCode());
		}
		return patientInvitationRevokeService.revoke(pending.getId(), nutritionistUserId);
	}

	private Paciente requireOwnedPaciente(final Long pacienteId, final String nutritionistUserId) {
		if (!StringUtils.hasText(nutritionistUserId)) {
			throw new IllegalArgumentException("nutritionistUserId is required");
		}
		return pacienteRepository.findByIdAndUserId(pacienteId, nutritionistUserId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
	}

	private java.util.Optional<PatientInvitation> findActivePendingInvitation(final Long pacienteId) {
		final Instant now = Instant.now();
		return patientInvitationRepository.findByPacienteIdAndStatus(pacienteId, PatientInvitationStatus.PENDING)
			.stream()
			.filter(invitation -> !invitation.getExpiresAt().isBefore(now))
			.findFirst();
	}

	private void revokeExpiredPendingInvitations(final Long pacienteId) {
		final Instant now = Instant.now();
		final List<PatientInvitation> expired = patientInvitationRepository
			.findByPacienteIdAndStatus(pacienteId, PatientInvitationStatus.PENDING)
			.stream()
			.filter(invitation -> invitation.getExpiresAt().isBefore(now))
			.toList();
		for (final PatientInvitation invitation : expired) {
			invitation.setStatus(PatientInvitationStatus.REVOKED);
			patientInvitationRepository.save(invitation);
		}
	}

	private static String requireRecipientEmail(final Paciente paciente) {
		final String email = PatientMobileInvitationUiSupport.resolveRecipientEmail(paciente);
		if (!StringUtils.hasText(email)) {
			throw new PatientMobileInvitationNotAllowedException("NO_EMAIL");
		}
		return email;
	}

	private void ensureInviteMetadata(final Paciente paciente) {
		if (!StringUtils.hasText(paciente.getAssignedId())) {
			String candidate = PatientAssignedIdGenerator.generate();
			while (pacienteRepository.existsByAssignedId(candidate)) {
				candidate = PatientAssignedIdGenerator.generate();
			}
			paciente.setAssignedId(candidate);
		}
		final String email = PatientMobileInvitationUiSupport.resolveRecipientEmail(paciente);
		if (StringUtils.hasText(email)) {
			paciente.setEmail(email);
			if (!StringUtils.hasText(paciente.getEmailHint())) {
				paciente.setEmailHint(email);
			}
		}
		if (!StringUtils.hasText(paciente.getDisplayName()) && StringUtils.hasText(paciente.getName())) {
			paciente.setDisplayName(paciente.getName().trim());
		}
		if (StringUtils.hasText(paciente.getGender())) {
			paciente.setGender(paciente.getGender().trim().toUpperCase(Locale.ROOT));
		}
	}

}
