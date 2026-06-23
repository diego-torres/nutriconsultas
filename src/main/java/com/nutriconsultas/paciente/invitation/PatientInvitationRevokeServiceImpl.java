package com.nutriconsultas.paciente.invitation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.mobile.PatientInvitationRevokeNotAllowedException;
import com.nutriconsultas.mobile.PatientInvitationRevokeNotFoundException;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationRevokeServiceImpl implements PatientInvitationRevokeService {

	private final PatientInvitationRepository patientInvitationRepository;

	public PatientInvitationRevokeServiceImpl(final PatientInvitationRepository patientInvitationRepository) {
		this.patientInvitationRepository = patientInvitationRepository;
	}

	@Override
	@Transactional
	public PatientInvitationRevokeResult revoke(final Long invitationId, final String nutritionistUserId) {
		final PatientInvitation invitation = patientInvitationRepository
			.findByIdAndNutritionistUserId(invitationId, nutritionistUserId)
			.orElseThrow(PatientInvitationRevokeNotFoundException::new);

		if (invitation.getStatus() == PatientInvitationStatus.REVOKED) {
			return toResult(invitation);
		}
		if (invitation.getStatus() != PatientInvitationStatus.PENDING) {
			throw new PatientInvitationRevokeNotAllowedException();
		}

		invitation.setStatus(PatientInvitationStatus.REVOKED);
		final PatientInvitation saved = patientInvitationRepository.save(invitation);
		if (log.isInfoEnabled()) {
			log.info("Revoked patient invitation: invitationId={}, pacienteId={}", saved.getId(),
					saved.getPaciente().getId());
		}
		return toResult(saved);
	}

	private static PatientInvitationRevokeResult toResult(final PatientInvitation invitation) {
		return new PatientInvitationRevokeResult(invitation.getId(), invitation.getPaciente().getId(),
				invitation.getStatus());
	}

}
