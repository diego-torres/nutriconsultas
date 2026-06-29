package com.nutriconsultas.mobile.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;

@Repository
public class PatientInvitationAuthRepository {

	private final PatientInvitationRepository patientInvitationRepository;

	public PatientInvitationAuthRepository(final PatientInvitationRepository patientInvitationRepository) {
		this.patientInvitationRepository = patientInvitationRepository;
	}

	@Transactional(readOnly = true)
	public Optional<PatientInvitation> findPendingByTokenHash(final String tokenHash) {
		return patientInvitationRepository.findWithPacienteByTokenHash(tokenHash).filter(this::isPending);
	}

	@Transactional(readOnly = true)
	public Optional<PatientInvitation> findPendingByHumanCode(final String humanCode) {
		return patientInvitationRepository.findWithPacienteByHumanCode(humanCode).filter(this::isPending);
	}

	private boolean isPending(final PatientInvitation invitation) {
		return invitation.getStatus() == PatientInvitationStatus.PENDING
				&& invitation.getExpiresAt().isAfter(Instant.now());
	}

}
