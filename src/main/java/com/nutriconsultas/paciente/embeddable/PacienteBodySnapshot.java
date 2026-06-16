package com.nutriconsultas.paciente.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.nutriconsultas.paciente.NivelPeso;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cached body metrics mirrored from the latest {@code BodyMetricRecord} (#156 Phase B).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteBodySnapshot {

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

	private NivelPeso nivelPeso;

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

	/**
	 * Additional energy from physiological stress in kcal/day.
	 */
	@Column(precision = 7)
	private Double stressKcal;

	/**
	 * Final daily energy requirement (GET + TEF + stress) in kcal/day.
	 */
	@Column(precision = 7)
	private Double finalTotalKcal;

}
