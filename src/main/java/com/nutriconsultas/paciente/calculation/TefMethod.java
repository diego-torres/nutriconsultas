package com.nutriconsultas.paciente.calculation;

/**
 * Method for calculating the thermic effect of food (TEF / ETA).
 */
public enum TefMethod {

	FIXED("Porcentaje fijo"), MACRONUTRIENTS("Por macronutrientes");

	private final String displayName;

	TefMethod(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
