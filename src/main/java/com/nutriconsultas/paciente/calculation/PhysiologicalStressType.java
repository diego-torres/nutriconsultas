package com.nutriconsultas.paciente.calculation;

/**
 * Catalog of physiological stress conditions affecting energy expenditure.
 */
public enum PhysiologicalStressType {

	NONE("Sin estrés", true), FEVER("Fiebre", true), MINOR_SURGERY("Cirugía menor", true),
	MAJOR_SURGERY("Cirugía mayor", true), MODERATE_INFECTION("Infección moderada", true),
	SEVERE_INFECTION("Infección severa", true), SEPSIS("Sepsis", true), TRAUMA("Trauma", true),
	BURNS("Quemaduras", true), CANCER("Cáncer", true), FRACTURE("Fractura", true),
	POST_OPERATIVE("Post-operatorio", true), MULTIPLE_TRAUMA("Politraumatismo", false),
	HEAD_INJURY("Traumatismo craneoencefálico", false), ORGAN_FAILURE("Fallo orgánico", false),
	PREGNANCY_COMPLICATION("Complicación obstétrica", false), COPD_EXACERBATION("Exacerbación EPOC", false),
	OTHER("Otro (factor personalizado)", false);

	private final String displayName;

	private final boolean common;

	PhysiologicalStressType(final String displayName, final boolean common) {
		this.displayName = displayName;
		this.common = common;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isCommon() {
		return common;
	}

}
