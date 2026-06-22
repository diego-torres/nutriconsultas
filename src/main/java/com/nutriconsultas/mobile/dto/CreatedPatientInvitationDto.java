package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.nutriconsultas.paciente.invitation.CreatedPatientInvitationResult;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after a nutritionist creates a patient invitation (#134). Contains the raw
 * invite URL and human code — never log these values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Created patient invitation")
public record CreatedPatientInvitationDto(@Schema(description = "Invitation row id", example = "42") Long invitationId,
		@Schema(description = "Created Paciente id", example = "1001") Long pacienteId,
		@Schema(description = "Deep link for the patient app",
				example = "https://links.example.com/i/abc...") String inviteUrl,
		@Schema(description = "Human-readable onboarding code", example = "NUTRI-ABCD-EFGH") String humanCode,
		@Schema(description = "Invitation expiry (ISO-8601 instant)") Instant expiresAt,
		@Schema(description = "Optional offline JWS when PATIENT_INVITATION_JWS_SECRET is configured") String offlineJws) {

	public static CreatedPatientInvitationDto from(final CreatedPatientInvitationResult result) {
		return new CreatedPatientInvitationDto(result.invitationId(), result.pacienteId(), result.inviteUrl(),
				result.humanCode(), result.expiresAt(), result.offlineJws());
	}

}
