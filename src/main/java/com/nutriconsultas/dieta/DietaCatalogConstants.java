package com.nutriconsultas.dieta;

/**
 * Ownership sentinel values for the shared diet catalog.
 */
public final class DietaCatalogConstants {

	public static final String SYSTEM_TEMPLATE_USER_ID = "system:template-dietas";

	private DietaCatalogConstants() {
	}

	public static boolean isSystemTemplate(final Dieta dieta) {
		return dieta != null && SYSTEM_TEMPLATE_USER_ID.equals(dieta.getUserId());
	}

	public static boolean isPatientAssignment(final Dieta dieta) {
		return dieta != null && dieta.getPacienteId() != null;
	}

	public static boolean isCatalogDieta(final Dieta dieta) {
		return dieta != null && dieta.getPacienteId() == null;
	}

}
