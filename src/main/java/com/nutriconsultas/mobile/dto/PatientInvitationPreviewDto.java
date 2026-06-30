package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.invitation.InvitationAuthPath;
import com.nutriconsultas.paciente.invitation.PatientInvitationPreviewResult;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public invitation preview (#135, #349). Inviter branding plus non-PII auth routing
 * hints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public invitation preview")
public record PatientInvitationPreviewDto(
		@Schema(description = "Nutritionist display name from profile",
				example = "Lic. Ana López") String inviterDisplayName,
		@Schema(description = "Patient lifecycle status", example = "INVITED") PacienteStatus patientStatus,
		@Schema(description = "Whether the patient record is linked to a mobile Auth0 account",
				example = "false") Boolean mobileAppLinked,
		@Schema(description = "Suggested Auth0 flow for the mobile app",
				example = "CREATE_ACCOUNT") InvitationAuthPath authPath,
		@Schema(description = "Masked email for optional pre-fill (never the full address)",
				example = "p***@example.com") String emailHint) {

	public static PatientInvitationPreviewDto from(final PatientInvitationPreviewResult result) {
		return new PatientInvitationPreviewDto(result.inviterDisplayName(), result.patientStatus(),
				result.mobileAppLinked(), result.authPath(), result.emailHint());
	}

}
