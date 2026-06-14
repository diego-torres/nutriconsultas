package com.nutriconsultas.paciente.calculation;

/**
 * Energy base used when applying a fixed TEF percentage.
 */
public enum TefBase {

	GET("GET (TMB × actividad)"), BMR("TMB");

	private final String displayName;

	TefBase(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
