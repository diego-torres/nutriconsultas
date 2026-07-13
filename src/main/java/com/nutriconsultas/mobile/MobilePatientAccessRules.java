package com.nutriconsultas.mobile;

import java.util.Optional;

import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

/**
 * Status-aware access rules for {@code /rest/mobile/patient/**} (#137).
 */
public final class MobilePatientAccessRules {

	public static final String PATIENT_API_PREFIX = "/rest/mobile/patient/";

	private static final String ONBOARDING_PROFILE_PATH = "/rest/mobile/patient/me";

	private static final String ONBOARDING_PROFILE_PHOTO_PATH = "/rest/mobile/patient/profile/photo";

	private MobilePatientAccessRules() {
	}

	public enum Decision {

		ALLOW, ONBOARDING_REQUIRED

	}

	public static Decision evaluatePatientApiAccess(final Optional<PacienteAuthView> authView, final String path) {
		if (path == null || !path.startsWith(PATIENT_API_PREFIX)) {
			return Decision.ALLOW;
		}
		if (authView.isEmpty()) {
			return Decision.ONBOARDING_REQUIRED;
		}
		return evaluateLinkedPatientAccess(authView.orElseThrow(), path);
	}

	private static Decision evaluateLinkedPatientAccess(final PacienteAuthView authView, final String path) {
		if (authView.getAppleLifecycleStatus() != null
				&& authView.getAppleLifecycleStatus() != ApplePacienteLifecycleStatus.NONE) {
			return Decision.ONBOARDING_REQUIRED;
		}
		final PacienteStatus status = authView.getStatus();
		if (status == PacienteStatus.REVOKED || status == PacienteStatus.INVITED) {
			return Decision.ONBOARDING_REQUIRED;
		}
		if (status == PacienteStatus.ONBOARDING) {
			return isOnboardingAllowedPath(path) ? Decision.ALLOW : Decision.ONBOARDING_REQUIRED;
		}
		if (status == PacienteStatus.ACTIVE) {
			return Decision.ALLOW;
		}
		return Decision.ONBOARDING_REQUIRED;
	}

	public static boolean isOnboardingAllowedPath(final String path) {
		return ONBOARDING_PROFILE_PATH.equals(path) || path.startsWith(ONBOARDING_PROFILE_PATH + "/")
				|| ONBOARDING_PROFILE_PHOTO_PATH.equals(path);
	}

}
