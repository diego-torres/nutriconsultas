package com.nutriconsultas.paciente.invitation;

import java.util.Optional;

/**
 * Resolves invitation landing page content for valid pending tokens (#337).
 */
@FunctionalInterface
public interface PatientInvitationLandingService {

	Optional<PatientInvitationLandingContent> resolve(String rawUrlToken, String clientKey);

}
