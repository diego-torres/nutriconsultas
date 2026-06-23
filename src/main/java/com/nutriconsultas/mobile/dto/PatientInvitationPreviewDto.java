package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.nutriconsultas.paciente.invitation.PatientInvitationPreviewResult;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public invitation preview (#135). Inviter branding only — no patient PII.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public invitation preview")
public record PatientInvitationPreviewDto(@Schema(description = "Nutritionist display name from profile",
		example = "Lic. Ana López") String inviterDisplayName) {

	public static PatientInvitationPreviewDto from(final PatientInvitationPreviewResult result) {
		return new PatientInvitationPreviewDto(result.inviterDisplayName());
	}

}
