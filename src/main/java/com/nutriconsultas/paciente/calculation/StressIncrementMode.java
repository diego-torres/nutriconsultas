package com.nutriconsultas.paciente.calculation;

/**
 * How physiological stress adjusts daily energy requirements.
 */
public enum StressIncrementMode {

	MULTIPLIER_BMR("Multiplicador sobre TMB"), MULTIPLIER_GET("Multiplicador sobre GET"),
	FIXED_KCAL("Suma fija de kcal/día");

	private final String displayName;

	StressIncrementMode(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
