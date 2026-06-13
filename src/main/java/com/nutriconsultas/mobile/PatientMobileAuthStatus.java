package com.nutriconsultas.mobile;

import java.util.Map;

/**
 * Status of patient mobile Auth0 linkage for admin UI and REST.
 */
public final class PatientMobileAuthStatus {

	private final boolean linked;

	private final String patientAuthSubRedacted;

	private final boolean emailLookupAvailable;

	private PatientMobileAuthStatus(final boolean linked, final String patientAuthSubRedacted,
			final boolean emailLookupAvailable) {
		this.linked = linked;
		this.patientAuthSubRedacted = patientAuthSubRedacted;
		this.emailLookupAvailable = emailLookupAvailable;
	}

	public static PatientMobileAuthStatus of(final String patientAuthSub, final boolean emailLookupAvailable) {
		final boolean linked = patientAuthSub != null && !patientAuthSub.isBlank();
		final String redacted = linked ? redactSub(patientAuthSub) : null;
		return new PatientMobileAuthStatus(linked, redacted, emailLookupAvailable);
	}

	public Map<String, Object> toMap() {
		return Map.of("linked", linked, "patientAuthSubRedacted",
				patientAuthSubRedacted != null ? patientAuthSubRedacted : "", "emailLookupAvailable",
				emailLookupAvailable);
	}

	public boolean isLinked() {
		return linked;
	}

	public String getPatientAuthSubRedacted() {
		return patientAuthSubRedacted;
	}

	public boolean isEmailLookupAvailable() {
		return emailLookupAvailable;
	}

	private static String redactSub(final String sub) {
		if (sub.length() <= 10) {
			return sub.substring(0, Math.min(3, sub.length())) + "…";
		}
		return sub.substring(0, 8) + "…" + sub.substring(sub.length() - 4);
	}

}
