package com.nutriconsultas.paciente.invitation;

/**
 * Nutritionist revokes an outstanding patient invitation (#139).
 */
@FunctionalInterface
public interface PatientInvitationRevokeService {

	PatientInvitationRevokeResult revoke(Long invitationId, String nutritionistUserId);

}
