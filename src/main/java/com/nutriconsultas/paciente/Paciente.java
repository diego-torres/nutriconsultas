package com.nutriconsultas.paciente;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.TefBase;
import com.nutriconsultas.paciente.calculation.TefCalculationService;
import com.nutriconsultas.paciente.calculation.TefMethod;
import com.nutriconsultas.paciente.validation.ValidPregnancy;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
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

	@Column(precision = 5)
	private Double peso;

	@Column(precision = 3)
	private Double estatura;

	@Column(precision = 3)
	private Double imc;

	/**
	 * Basal metabolic rate (BMR) in kcal/day.
	 */
	@Column(precision = 7)
	private Double bmr;

	/**
	 * Total daily energy expenditure (GET) in kcal/day.
	 */
	@Column(precision = 7)
	private Double getKcal;

	/**
	 * Thermic effect of food (TEF / ETA) in kcal/day.
	 */
	@Column(precision = 7)
	private Double tefKcal;

	/**
	 * Total adjusted daily energy requirement (GET + TEF) in kcal/day.
	 */
	@Column(precision = 7)
	private Double totalAdjustedKcal;

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

	private NivelPeso nivelPeso;

	// ANTECEDENTES
	@Column(columnDefinition = "TEXT")
	private String antecedentesPrenatales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesNatales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesPatologicosPersonales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesPatologicosFamiliares;

	@Column(columnDefinition = "TEXT")
	private String complicaciones;

	@Column(length = 4)
	private String tipoSanguineo;

	// NUTRICION Y DESARROLLO
	@Column(columnDefinition = "TEXT")
	private String historialAlimenticio;

	@Column(columnDefinition = "TEXT")
	private String desarrolloPsicomotor;

	@Column(columnDefinition = "TEXT")
	private String alergias;

	// BANDERAS DE PATOLOGIAS COMUNES
	private Boolean hipertension = false;

	private Boolean diabetes = false;

	private Boolean hipotiroidismo = false;

	private Boolean obesidad = false;

	private Boolean anemia = false;

	private Boolean bulimia = false;

	private Boolean anorexia = false;

	private Boolean enfermedadesHepaticas = false;

	// ESTADO DE EMBARAZO (solo para mujeres entre 12-50 años)
	@ValidPregnancy
	private Boolean pregnancy = false;

}
