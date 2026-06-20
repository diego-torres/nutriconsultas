package com.nutriconsultas.platillos;

/**
 * Ownership filter for the platillo catalog grid.
 */
public enum PlatilloCatalogFilter {

	TODAS, SISTEMA, PROPIAS;

	public static PlatilloCatalogFilter fromRequestValue(final String value) {
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
