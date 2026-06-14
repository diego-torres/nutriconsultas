package com.nutriconsultas.paciente.calculation;

/**
 * Clinical reference table used to derive physiological stress factors.
 */
public enum StressFormulaTable {

	LONG("Factores de Long"), FEVER_PER_DEGREE("Incremento por °C de fiebre (13% TMB/°C)"), ASPEN("Factores ASPEN"),
	CUSTOM("Personalizado");

	private final String displayName;

	StressFormulaTable(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
