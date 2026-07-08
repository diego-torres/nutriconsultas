package com.nutriconsultas.platillos;

/**
 * Picture filter for the platillo catalog grid.
 */
public enum PlatilloPictureFilter {

	TODAS, SIN_IMAGEN;

	public static PlatilloPictureFilter fromRequestValue(final String value) {
		if (value == null || value.isBlank()) {
			return TODAS;
		}
		final String normalized = value.trim().toLowerCase();
		if ("sin-imagen".equals(normalized) || "sin_imagen".equals(normalized) || "no-picture".equals(normalized)) {
			return SIN_IMAGEN;
		}
		return TODAS;
	}

}
