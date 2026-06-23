package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.invitation.PatientInvitationRevokeResult;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after a nutritionist revokes a patient invitation (#139).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Revoked patient invitation")
public record RevokedPatientInvitationDto(@Schema(description = "Invitation row id", example = "42") Long invitationId,
		@Schema(description = "Linked Paciente id", example = "1001") Long pacienteId,
		@Schema(description = "Invitation status after revoke", example = "REVOKED") PatientInvitationStatus status) {

	public static RevokedPatientInvitationDto from(final PatientInvitationRevokeResult result) {
		return new RevokedPatientInvitationDto(result.invitationId(), result.pacienteId(), result.status());
	}

}
