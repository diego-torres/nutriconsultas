package com.nutriconsultas.auth.apple;

/**
 * Mapping outcome for Apple Sign-In notifications (#504).
 */
public record AppleIdentityMappingResult(AppleIdentityMappingStatus status, String auth0UserId, Long pacienteId,
		String detail) {

	public static AppleIdentityMappingResult notAttempted() {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.NOT_ATTEMPTED, null, null, null);
	}

	public static AppleIdentityMappingResult noAppleSubject() {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.NO_APPLE_SUBJECT, null, null, null);
	}

	public static AppleIdentityMappingResult mapped(final String auth0UserId, final Long pacienteId) {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.MAPPED, auth0UserId, pacienteId, null);
	}

	public static AppleIdentityMappingResult noAuth0User() {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.NO_AUTH0_USER, null, null, null);
	}

	public static AppleIdentityMappingResult noLocalUser(final String auth0UserId) {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.NO_LOCAL_USER, auth0UserId, null, null);
	}

	public static AppleIdentityMappingResult ambiguous(final String detail) {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.AMBIGUOUS, null, null, detail);
	}

	public static AppleIdentityMappingResult auth0LookupFailed(final String detail) {
		return new AppleIdentityMappingResult(AppleIdentityMappingStatus.AUTH0_LOOKUP_FAILED, null, null, detail);
	}

}
