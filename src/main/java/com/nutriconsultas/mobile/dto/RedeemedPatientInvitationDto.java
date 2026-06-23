package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.invitation.PatientInvitationRedeemResult;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after a patient redeems an onboarding invitation (#136).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Redeemed patient invitation")
public record RedeemedPatientInvitationDto(
		@Schema(description = "Linked Paciente id", example = "1001") Long pacienteId,
		@Schema(description = "Patient lifecycle status after redeem",
				example = "ONBOARDING") PacienteStatus pacienteStatus,
		@Schema(description = "Invitation row id", example = "42") Long invitationId,
		@Schema(description = "Redemption timestamp (ISO-8601 instant)") Instant redeemedAt) {

	public static RedeemedPatientInvitationDto from(final PatientInvitationRedeemResult result) {
		return new RedeemedPatientInvitationDto(result.pacienteId(), result.pacienteStatus(), result.invitationId(),
				result.redeemedAt());
	}

}
