package com.nutriconsultas.dieta;

/**
 * Ownership filter for the diet catalog grid.
 */
public enum DietaCatalogFilter {

	TODAS, SISTEMA, PROPIAS;

	public static DietaCatalogFilter fromRequestValue(final String value) {
		if (value == null || value.isBlank()) {
			return TODAS;
		}
		final String normalized = value.trim().toLowerCase();
		if ("sistema".equals(normalized)) {
			return SISTEMA;
		}
		if ("propias".equals(normalized) || "mis".equals(normalized)) {
			return PROPIAS;
		}
		return TODAS;
	}

}
