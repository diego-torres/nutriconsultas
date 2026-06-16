package com.nutriconsultas.clinical.exam.anthropometric;

/**
 * Clinical source used to obtain consolidated body fat / muscle composition values.
 */
public enum MetodoObtencionComposicionCorporal {

	MANUAL("Manual"), BIOIMPEDANCIA("Bioimpedancia"), PLIEGUES("Pliegues (Jackson-Pollock)"),
	DEURENBERG("Deurenberg (estimado)"), DEXA("DEXA"), OTRO("Otro");

	private final String displayName;

	MetodoObtencionComposicionCorporal(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isClinicalOverride() {
		return this == DEXA || this == OTRO;
	}

}
