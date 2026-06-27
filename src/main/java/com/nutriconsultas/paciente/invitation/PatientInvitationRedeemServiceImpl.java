package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationPatientStatusException;
import com.nutriconsultas.mobile.PatientInvitationRedeemConflictException;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationRedeemServiceImpl implements PatientInvitationRedeemService {

	private final PatientInvitationRepository patientInvitationRepository;

	private final PacienteRepository pacienteRepository;

	private final PatientInvitationProperties invitationProperties;

	public PatientInvitationRedeemServiceImpl(final PatientInvitationRepository patientInvitationRepository,
			final PacienteRepository pacienteRepository, final PatientInvitationProperties invitationProperties) {
		this.patientInvitationRepository = patientInvitationRepository;
		this.pacienteRepository = pacienteRepository;
		this.invitationProperties = invitationProperties;
	}

	@Override
	@Transactional
	public PatientInvitationRedeemResult redeem(final String rawUrlToken, final String patientAuthSub) {
		if (!StringUtils.hasText(patientAuthSub)) {
			throw new IllegalArgumentException("patientAuthSub is required");
		}
		if (!PatientInvitationUrlTokens.isWellFormed(rawUrlToken)) {
			throw new PatientInvitationInvalidTokenException();
		}
		final String tokenHash = InvitationTokenHasher.hashToken(rawUrlToken);
		final PatientInvitation invitation = patientInvitationRepository.findByTokenHash(tokenHash)
			.orElseThrow(PatientInvitationUnavailableException::new);
		return redeemInvitation(invitation, patientAuthSub);
	}

	@Override
	@Transactional
	public PatientInvitationRedeemResult redeemByHumanCode(final String humanCode, final String patientAuthSub) {
		if (!StringUtils.hasText(patientAuthSub)) {
			throw new IllegalArgumentException("patientAuthSub is required");
		}
		if (!PatientInvitationHumanCodes.isWellFormed(humanCode, invitationProperties.getHumanCodePrefix())) {
			throw new PatientInvitationInvalidTokenException();
		}
		final String normalizedCode = PatientInvitationHumanCodes.normalize(humanCode);
		final PatientInvitation invitation = patientInvitationRepository.findByHumanCode(normalizedCode)
			.orElseThrow(PatientInvitationUnavailableException::new);
		return redeemInvitation(invitation, patientAuthSub);
	}

	@Override
	@Transactional
	public PatientInvitationRedeemResult reconcile(final PatientInvitationReconcileInput input) {
		if (!StringUtils.hasText(input.patientAuthSub())) {
			throw new IllegalArgumentException("patientAuthSub is required");
		}
		final String patientAuthSub = input.patientAuthSub();

		final Optional<PacienteAuthView> linkedPatient = pacienteRepository
			.findAuthViewByPatientAuthSub(patientAuthSub);
		if (linkedPatient.isPresent()) {
			return alreadyLinkedResult(linkedPatient.get());
		}

		if (StringUtils.hasText(input.rawUrlToken())) {
			return redeem(input.rawUrlToken(), patientAuthSub);
		}
		if (StringUtils.hasText(input.humanCode())) {
			return redeemByHumanCode(input.humanCode(), patientAuthSub);
		}

		final List<PatientInvitation> redeemedInvitations = patientInvitationRepository
			.findRedeemedByPatientAuthSub(patientAuthSub, PatientInvitationStatus.REDEEMED);
		if (!redeemedInvitations.isEmpty()) {
			return repairPacienteLinkage(redeemedInvitations.get(0), patientAuthSub);
		}

		if (!StringUtils.hasText(input.email())) {
			throw new PatientInvitationUnavailableException();
		}

		final List<PatientInvitation> pendingInvitations = patientInvitationRepository
			.findRedeemablePendingByPacienteEmail(input.email().trim(), Instant.now(), PatientInvitationStatus.PENDING,
					PacienteStatus.INVITED);
		if (pendingInvitations.isEmpty()) {
			throw new PatientInvitationUnavailableException();
		}
		if (pendingInvitations.size() > 1 && log.isWarnEnabled()) {
			log.warn("Multiple pending invitations match JWT email; reconciling invitationId={}",
					pendingInvitations.get(0).getId());
		}
		return redeemInvitation(pendingInvitations.get(0), patientAuthSub);
	}

	private PatientInvitationRedeemResult alreadyLinkedResult(final PacienteAuthView linkedPatient) {
		final List<PatientInvitation> redeemedInvitations = patientInvitationRepository
			.findByPacienteIdAndStatus(linkedPatient.getId(), PatientInvitationStatus.REDEEMED);
		Long invitationId = null;
		Instant redeemedAt = null;
		if (!redeemedInvitations.isEmpty()) {
			final PatientInvitation invitation = redeemedInvitations.get(0);
			invitationId = invitation.getId();
			redeemedAt = invitation.getRedeemedAt();
		}
		return new PatientInvitationRedeemResult(linkedPatient.getId(), linkedPatient.getStatus(), invitationId,
				redeemedAt);
	}

	private PatientInvitationRedeemResult repairPacienteLinkage(final PatientInvitation invitation,
			final String patientAuthSub) {
		if (!Objects.equals(patientAuthSub, invitation.getRedeemedBySub())) {
			throw new PatientInvitationRedeemConflictException();
		}
		final Paciente paciente = pacienteRepository.findById(invitation.getPaciente().getId())
			.orElseThrow(PatientInvitationUnavailableException::new);
		if (!StringUtils.hasText(paciente.getPatientAuthSub())) {
			rejectIfSubLinkedElsewhere(patientAuthSub, paciente.getId());
			paciente.setPatientAuthSub(patientAuthSub);
			if (paciente.getStatus() == PacienteStatus.INVITED) {
				paciente.setStatus(PacienteStatus.ONBOARDING);
			}
			pacienteRepository.save(paciente);
			if (log.isInfoEnabled()) {
				log.info("Repaired patient auth linkage from redeemed invitation: invitationId={}, pacienteId={}",
						invitation.getId(), paciente.getId());
			}
		}
		return new PatientInvitationRedeemResult(paciente.getId(), paciente.getStatus(), invitation.getId(),
				invitation.getRedeemedAt());
	}

	private PatientInvitationRedeemResult redeemInvitation(final PatientInvitation invitation,
			final String patientAuthSub) {
		if (invitation.getStatus() == PatientInvitationStatus.REDEEMED) {
			return handleAlreadyRedeemed(invitation, patientAuthSub);
		}
		if (!isRedeemablePending(invitation)) {
			throw new PatientInvitationUnavailableException();
		}

		rejectIfSubLinkedElsewhere(patientAuthSub, invitation.getPaciente().getId());

		final Paciente paciente = pacienteRepository.findById(invitation.getPaciente().getId())
			.orElseThrow(PatientInvitationUnavailableException::new);
		if (paciente.getStatus() != PacienteStatus.INVITED) {
			throw new PatientInvitationPatientStatusException();
		}
		if (StringUtils.hasText(paciente.getPatientAuthSub())
				&& !Objects.equals(patientAuthSub, paciente.getPatientAuthSub())) {
			throw new PatientInvitationRedeemConflictException();
		}

		final Instant redeemedAt = Instant.now();
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setStatus(PacienteStatus.ONBOARDING);
		invitation.setStatus(PatientInvitationStatus.REDEEMED);
		invitation.setRedeemedBySub(patientAuthSub);
		invitation.setRedeemedAt(redeemedAt);
		pacienteRepository.save(paciente);
		patientInvitationRepository.save(invitation);

		if (log.isInfoEnabled()) {
			log.info("Redeemed patient invitation: invitationId={}, pacienteId={}", invitation.getId(),
					paciente.getId());
		}

		return new PatientInvitationRedeemResult(paciente.getId(), PacienteStatus.ONBOARDING, invitation.getId(),
				redeemedAt);
	}

	private PatientInvitationRedeemResult handleAlreadyRedeemed(final PatientInvitation invitation,
			final String patientAuthSub) {
		if (!Objects.equals(patientAuthSub, invitation.getRedeemedBySub())) {
			throw new PatientInvitationRedeemConflictException();
		}
		final Paciente paciente = invitation.getPaciente();
		return new PatientInvitationRedeemResult(paciente.getId(), paciente.getStatus(), invitation.getId(),
				invitation.getRedeemedAt());
	}

	private static boolean isRedeemablePending(final PatientInvitation invitation) {
		return invitation.getStatus() == PatientInvitationStatus.PENDING
				&& !invitation.getExpiresAt().isBefore(Instant.now());
	}

	private void rejectIfSubLinkedElsewhere(final String patientAuthSub, final Long invitationPacienteId) {
		pacienteRepository.findByPatientAuthSub(patientAuthSub).ifPresent(existing -> {
			if (!existing.getId().equals(invitationPacienteId)) {
				throw new PatientInvitationRedeemConflictException();
			}
		});
	}

}
