package com.nutriconsultas.paciente.invitation;

/**
 * Public, rate-limited invitation preview for patient onboarding (#135).
 */
@FunctionalInterface
public interface PatientInvitationPreviewService {

	PatientInvitationPreviewResult preview(String rawUrlToken);

}
