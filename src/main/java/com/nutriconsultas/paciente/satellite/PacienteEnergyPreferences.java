package com.nutriconsultas.paciente.satellite;

import java.util.Date;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.PhysiologicalStressType;
import com.nutriconsultas.paciente.calculation.StressFormulaTable;
import com.nutriconsultas.paciente.calculation.StressIncrementMode;
import com.nutriconsultas.paciente.calculation.TefBase;
import com.nutriconsultas.paciente.calculation.TefCalculationService;
import com.nutriconsultas.paciente.calculation.TefMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Energy-expenditure preferences (GET/TDEE, TEF, activity, stress) — #156 Phase C
 * satellite table.
 */
@Entity
@Table(name = "paciente_energy_preferences")
@Getter
@Setter
@NoArgsConstructor
public class PacienteEnergyPreferences {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "paciente_id", nullable = false, unique = true)
	private Paciente paciente;

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
