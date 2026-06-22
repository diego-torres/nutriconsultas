package com.nutriconsultas.paciente.invitation;

import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;

/**
 * Nutritionist-initiated patient onboarding invitations (#134).
 */
@FunctionalInterface
public interface PatientInvitationCreateService {

	CreatedPatientInvitationResult createInvitation(String nutritionistUserId, CreatePatientInvitationRequest request);

}
