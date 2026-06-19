package com.nutriconsultas.paciente.mpx;

import lombok.Getter;
import lombok.Setter;

/**
 * Energy-expenditure preferences for MPX v1 (#221).
 */
@Getter
@Setter
public class MpxEnergyPreferences {

	private String activityFactorScale;

	private String preferredBmrFormula;

	private String physicalActivityLevel;

	private Double activityFactor;

	private Double customFactorSedentary;

	private Double customFactorLight;

	private Double customFactorModerate;

	private Double customFactorIntense;

	private Double customFactorVeryIntense;

	private Boolean physiologicalStressActive;

	private String physiologicalStressType;

	private String stressFormulaTable;

	private String stressIncrementMode;

	private Double stressFactorValue;

	private String stressValidFrom;

	private String stressValidUntil;

	private Double stressFeverTemperature;

	private String tefMethod;

	private String tefBase;

	private Double tefFixedPercent;

	private Double tefMacroProteinPercent;

	private Double tefMacroCarbsPercent;

	private Double tefMacroFatPercent;

}
