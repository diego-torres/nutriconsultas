package com.nutriconsultas.paciente;

import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.nutriconsultas.paciente.embeddable.PacienteBodySnapshot;
import com.nutriconsultas.paciente.embeddable.PacienteEnergyPreferences;
import com.nutriconsultas.paciente.embeddable.PacienteMedicalHistory;
import com.nutriconsultas.paciente.validation.ValidPregnancy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "El nombre es requerido")
	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String userId;

	/**
	 * Patient Auth0 {@code sub} claim for mobile JWT identity. Distinct from
	 * {@link #userId} (nutritionist tenant owner). Formal Liquibase changeset tracked in
	 * issue #46.
	 */
	@Column(unique = true, length = 255)
	private String patientAuthSub;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	@NotNull(message = "La fecha de nacimiento es requerida")
	@Column(nullable = false)
	private Date dob;

	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date registro = new Date();

	@Column(length = 100)
	private String email;

	@Column(length = 25)
	private String phone;

	@NotBlank(message = "El género es requerido")
	@Column(nullable = false, length = 1)
	private String gender;

	@Column(length = 100)
	private String responsibleName;

	private String parentesco;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "peso", column = @Column(name = "peso")),
			@AttributeOverride(name = "estatura", column = @Column(name = "estatura")),
			@AttributeOverride(name = "imc", column = @Column(name = "imc")),
			@AttributeOverride(name = "bmr", column = @Column(name = "bmr")),
			@AttributeOverride(name = "getKcal", column = @Column(name = "get_kcal")),
			@AttributeOverride(name = "nivelPeso", column = @Column(name = "nivel_peso")),
			@AttributeOverride(name = "tefKcal", column = @Column(name = "tef_kcal")),
			@AttributeOverride(name = "totalAdjustedKcal", column = @Column(name = "total_adjusted_kcal")),
			@AttributeOverride(name = "stressKcal", column = @Column(name = "stress_kcal")),
			@AttributeOverride(name = "finalTotalKcal", column = @Column(name = "final_total_kcal")) })
	@Delegate
	private PacienteBodySnapshot bodySnapshot = new PacienteBodySnapshot();

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "activityFactorScale", column = @Column(name = "activity_factor_scale")),
			@AttributeOverride(name = "preferredBmrFormula", column = @Column(name = "preferred_bmr_formula")),
			@AttributeOverride(name = "physicalActivityLevel", column = @Column(name = "physical_activity_level")),
			@AttributeOverride(name = "activityFactor", column = @Column(name = "activity_factor")),
			@AttributeOverride(name = "customFactorSedentary", column = @Column(name = "custom_factor_sedentary")),
			@AttributeOverride(name = "customFactorLight", column = @Column(name = "custom_factor_light")),
			@AttributeOverride(name = "customFactorModerate", column = @Column(name = "custom_factor_moderate")),
			@AttributeOverride(name = "customFactorIntense", column = @Column(name = "custom_factor_intense")),
			@AttributeOverride(name = "customFactorVeryIntense", column = @Column(name = "custom_factor_very_intense")),
			@AttributeOverride(name = "physiologicalStressActive",
					column = @Column(name = "physiological_stress_active")),
			@AttributeOverride(name = "physiologicalStressType", column = @Column(name = "physiological_stress_type")),
			@AttributeOverride(name = "stressFormulaTable", column = @Column(name = "stress_formula_table")),
			@AttributeOverride(name = "stressIncrementMode", column = @Column(name = "stress_increment_mode")),
			@AttributeOverride(name = "stressFactorValue", column = @Column(name = "stress_factor_value")),
			@AttributeOverride(name = "stressValidFrom", column = @Column(name = "stress_valid_from")),
			@AttributeOverride(name = "stressValidUntil", column = @Column(name = "stress_valid_until")),
			@AttributeOverride(name = "stressFeverTemperature", column = @Column(name = "stress_fever_temperature")),
			@AttributeOverride(name = "tefMethod", column = @Column(name = "tef_method")),
			@AttributeOverride(name = "tefBase", column = @Column(name = "tef_base")),
			@AttributeOverride(name = "tefFixedPercent", column = @Column(name = "tef_fixed_percent")),
			@AttributeOverride(name = "tefMacroProteinPercent", column = @Column(name = "tef_macro_protein_percent")),
			@AttributeOverride(name = "tefMacroCarbsPercent", column = @Column(name = "tef_macro_carbs_percent")),
			@AttributeOverride(name = "tefMacroFatPercent", column = @Column(name = "tef_macro_fat_percent")) })
	@Delegate
	private PacienteEnergyPreferences energyPreferences = new PacienteEnergyPreferences();

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "antecedentesPrenatales", column = @Column(name = "antecedentes_prenatales")),
			@AttributeOverride(name = "antecedentesNatales", column = @Column(name = "antecedentes_natales")),
			@AttributeOverride(name = "antecedentesPatologicosPersonales",
					column = @Column(name = "antecedentes_patologicos_personales")),
			@AttributeOverride(name = "antecedentesPatologicosFamiliares",
					column = @Column(name = "antecedentes_patologicos_familiares")),
			@AttributeOverride(name = "complicaciones", column = @Column(name = "complicaciones")),
			@AttributeOverride(name = "tipoSanguineo", column = @Column(name = "tipo_sanguineo")),
			@AttributeOverride(name = "historialAlimenticio", column = @Column(name = "historial_alimenticio")),
			@AttributeOverride(name = "desarrolloPsicomotor", column = @Column(name = "desarrollo_psicomotor")),
			@AttributeOverride(name = "alergias", column = @Column(name = "alergias")),
			@AttributeOverride(name = "hipertension", column = @Column(name = "hipertension")),
			@AttributeOverride(name = "diabetes", column = @Column(name = "diabetes")),
			@AttributeOverride(name = "hipotiroidismo", column = @Column(name = "hipotiroidismo")),
			@AttributeOverride(name = "obesidad", column = @Column(name = "obesidad")),
			@AttributeOverride(name = "anemia", column = @Column(name = "anemia")),
			@AttributeOverride(name = "bulimia", column = @Column(name = "bulimia")),
			@AttributeOverride(name = "anorexia", column = @Column(name = "anorexia")),
			@AttributeOverride(name = "enfermedadesHepaticas", column = @Column(name = "enfermedades_hepaticas")) })
	@Delegate
	private PacienteMedicalHistory medicalHistory = new PacienteMedicalHistory();

	// ESTADO DE EMBARAZO (solo para mujeres entre 12-50 años)
	@ValidPregnancy
	private Boolean pregnancy = false;

}
