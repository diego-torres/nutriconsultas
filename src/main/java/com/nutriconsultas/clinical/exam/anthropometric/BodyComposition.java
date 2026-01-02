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
public class BodyComposition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 3)
	private Double porcentajeGrasaCorporal; // % grasa corporal

	@Column(precision = 3)
	private Double indiceGrasaCorporal; // Índice de grasa corporal (centralized from BodyMass and VitalSigns)

	@Column(precision = 3)
	private Double porcentajeMasaMuscular;

}

