package com.nutriconsultas.clinical.exam.anthropometric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bioimpedance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double bioimpedanceValue;

	// Bioimpedance measurements
	@Column(precision = 5)
	private Double impedance; // Impedancia (Ω)

	@Column(precision = 5)
	private Double resistance; // Resistencia (R, Ω)

	@Column(precision = 5)
	private Double reactance; // Reactancia (Xc, Ω)

	@Column(precision = 3)
	private Double phaseAngle; // Ángulo de fase (°)

	// Body composition from bioimpedance
	@Column(precision = 3)
	private Double bodyFatPercentage; // % grasa corporal

	@Column(precision = 5)
	private Double fatMass; // Masa grasa (kg)

	@Column(precision = 5)
	private Double fatFreeMass; // Masa libre de grasa (kg)

	@Column(precision = 5)
	private Double skeletalMuscleMass; // Masa muscular esquelética (kg)

	@Column(precision = 5)
	private Double totalBodyWaterLiters; // Agua corporal total (L)

	@Column(precision = 3)
	private Double totalBodyWaterPercentage; // Agua corporal total (%)

	@Column(precision = 5)
	private Double intracellularWater; // Agua intracelular (L)

	@Column(precision = 5)
	private Double extracellularWater; // Agua extracelular (L)

	@Column(precision = 3)
	private Double ecwIcwRatio; // Relación ECW/ICW

	@Column(precision = 5)
	private Double estimatedBasalMetabolism; // Metabolismo basal estimado (kcal)

}

