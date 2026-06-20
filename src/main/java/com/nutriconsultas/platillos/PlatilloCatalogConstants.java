package com.nutriconsultas.platillos;

/**
 * Ownership sentinel values for the shared platillo catalog.
 */
public final class PlatilloCatalogConstants {

	public static final String SYSTEM_CATALOG_USER_ID = "system:catalog-platillos";

	private PlatilloCatalogConstants() {
	}

	public static boolean isSystemCatalog(final Platillo platillo) {
		return platillo != null && SYSTEM_CATALOG_USER_ID.equals(platillo.getUserId());
	}

}
