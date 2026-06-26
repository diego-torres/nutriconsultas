package com.nutriconsultas.paciente.invitation;

/**
 * Public, rate-limited invitation preview for patient onboarding (#135, #336).
 */
public interface PatientInvitationPreviewService {

	PatientInvitationPreviewResult preview(String rawUrlToken);

	PatientInvitationPreviewResult previewByHumanCode(String humanCode);

}
