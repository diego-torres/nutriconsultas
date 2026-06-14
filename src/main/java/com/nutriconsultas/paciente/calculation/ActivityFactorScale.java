package com.nutriconsultas.paciente.calculation;

/**
 * Catalog of physical-activity factor scales for GET (TDEE) calculation.
 *
 * <p>
 * GET = TMB (BMR) × factor de actividad física. Each scale defines five standard activity
 * levels; {@link #CUSTOM} uses per-patient factors configured by the nutritionist.
 */
public enum ActivityFactorScale {

	/**
	 * Harris-Benedict revised (1990) activity multipliers — widely used in clinical
	 * nutrition software.
	 */
	HARRIS_BENEDICT("Harris-Benedict"),

	/**
	 * FAO/WHO/UNU (1985) physical activity level (PAL) coefficients for adults.
	 */
	FAO_WHO("FAO/OMS 1985"),

	/**
	 * WHO (2004) PAL categories for adult energy requirements.
	 */
	OMS("OMS/WHO 2004"),

	/**
	 * Nutritionist-defined factors stored on the patient record.
	 */
	CUSTOM("Personalizado");

	private final String displayName;

	ActivityFactorScale(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
