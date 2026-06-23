package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.util.Objects;

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
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationRedeemServiceImpl implements PatientInvitationRedeemService {

	private final PatientInvitationRepository patientInvitationRepository;

	private final PacienteRepository pacienteRepository;

	public PatientInvitationRedeemServiceImpl(final PatientInvitationRepository patientInvitationRepository,
			final PacienteRepository pacienteRepository) {
		this.patientInvitationRepository = patientInvitationRepository;
		this.pacienteRepository = pacienteRepository;
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
