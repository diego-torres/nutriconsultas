package com.nutriconsultas.paciente.invitation;

public interface PatientMobileInvitationService {

	PatientMobileInvitationStatus getStatus(Long pacienteId, String nutritionistUserId);

	IssuedPatientMobileInvitationResult sendInvitation(Long pacienteId, String nutritionistUserId);

	PatientInvitationRevokeResult revokePendingInvitation(Long pacienteId, String nutritionistUserId);

}
