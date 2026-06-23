package com.nutriconsultas.mobile;

import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;

/**
 * Required-field checks for mobile onboarding profile completion (#138).
 */
public final class PatientOnboardingCompleteness {

	private PatientOnboardingCompleteness() {
	}

	public static boolean isComplete(final Paciente paciente) {
		if (paciente == null) {
			return false;
		}
		return StringUtils.hasText(paciente.getName()) && paciente.getDob() != null
				&& isValidGender(paciente.getGender()) && StringUtils.hasText(paciente.getDisplayName())
				&& PacienteAvatarCatalog.isValid(paciente.getAvatarId());
	}

	private static boolean isValidGender(final String gender) {
		if (!StringUtils.hasText(gender)) {
			return false;
		}
		final String normalized = gender.trim().toUpperCase();
		return "M".equals(normalized) || "F".equals(normalized);
	}

}
