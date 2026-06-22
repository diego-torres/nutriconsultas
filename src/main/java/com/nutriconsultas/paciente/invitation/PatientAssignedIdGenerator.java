package com.nutriconsultas.paciente.invitation;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates unique clinic-facing {@code assignedId} values when the nutritionist does not
 * supply one at invite time (#134).
 */
public final class PatientAssignedIdGenerator {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private PatientAssignedIdGenerator() {
	}

	public static String generate() {
		final byte[] bytes = new byte[6];
		SECURE_RANDOM.nextBytes(bytes);
		return "P-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
