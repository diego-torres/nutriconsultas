package com.nutriconsultas.paciente.embeddable;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.PhysiologicalStressType;
import com.nutriconsultas.paciente.calculation.StressFormulaTable;
import com.nutriconsultas.paciente.calculation.StressIncrementMode;
import com.nutriconsultas.paciente.calculation.TefBase;
import com.nutriconsultas.paciente.calculation.TefCalculationService;
import com.nutriconsultas.paciente.calculation.TefMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Patient energy-expenditure preferences (GET/TDEE, TEF, activity factors, stress) — #156
 * Phase B.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteEnergyPreferences {

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private ActivityFactorScale activityFactorScale = ActivityFactorScale.HARRIS_BENEDICT;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private BmrFormulaType preferredBmrFormula = BmrFormulaType.PROMEDIO;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private PhysicalActivityLevel physicalActivityLevel;

	@Column(precision = 4)
	private Double activityFactor;

	@Column(precision = 4)
	private Double customFactorSedentary;

	@Column(precision = 4)
	private Double customFactorLight;

	@Column(precision = 4)
	private Double customFactorModerate;

	@Column(precision = 4)
	private Double customFactorIntense;

	@Column(precision = 4)
	private Double customFactorVeryIntense;

	private Boolean physiologicalStressActive = false;

	@Enumerated(EnumType.STRING)
	@Column(length = 40)
	private PhysiologicalStressType physiologicalStressType;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private StressFormulaTable stressFormulaTable = StressFormulaTable.LONG;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private StressIncrementMode stressIncrementMode = StressIncrementMode.MULTIPLIER_BMR;

	@Column(precision = 5)
	private Double stressFactorValue;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	private Date stressValidFrom;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	private Date stressValidUntil;

	@Column(precision = 4)
	private Double stressFeverTemperature;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private TefMethod tefMethod = TefMethod.FIXED;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private TefBase tefBase = TefBase.GET;

	@Column(precision = 5)
	private Double tefFixedPercent = TefCalculationService.DEFAULT_FIXED_TEF_PERCENT;

	@Column(precision = 5)
	private Double tefMacroProteinPercent;

	@Column(precision = 5)
	private Double tefMacroCarbsPercent;

	@Column(precision = 5)
	private Double tefMacroFatPercent;

}
