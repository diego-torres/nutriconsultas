package com.nutriconsultas.paciente.calculation;

/**
 * Standard physical activity levels used to select an activity factor from a scale.
 */
public enum PhysicalActivityLevel {

	SEDENTARY("Sedentario"), LIGHT("Ligera"), MODERATE("Moderada"), INTENSE("Intensa"), VERY_INTENSE("Muy intensa"),
	CUSTOM("Personalizado");

	private final String displayName;

	PhysicalActivityLevel(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
