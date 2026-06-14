package com.nutriconsultas.paciente.calculation;

/**
 * BMR (TMB) formula used as the basis for GET calculation.
 */
public enum BmrFormulaType {

	MIFFLIN_ST_JEOR("Mifflin-St Jeor"), HARRIS_BENEDICT("Harris-Benedict"), FAO_WHO_ONU("FAO/OMS/ONU"),
	VALENCIA("Valencia"), PROMEDIO("Promedio");

	private final String displayName;

	BmrFormulaType(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
