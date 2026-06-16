package com.nutriconsultas.clinical.exam.anthropometric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	private Double indiceGrasaCorporal; // Índice de grasa corporal (centralized from
										// BodyMass and VitalSigns)

	@Column(precision = 3)
	private Double porcentajeMasaMuscular;

	@Column(precision = 5)
	private Double masaOseaKg;

	@Column(precision = 3)
	private Double porcentajeMasaOsea;

	/** Heath-Carter endomorphy rating (adults). */
	@Column(precision = 4)
	private Double endomorphy;

	/** Heath-Carter mesomorphy rating (adults). */
	@Column(precision = 4)
	private Double mesomorphy;

	/** Heath-Carter ectomorphy rating (adults). */
	@Column(precision = 4)
	private Double ectomorphy;

	/** Somatocarta X: ectomorphy - endomorphy. */
	@Column(precision = 4)
	private Double somatocartaX;

	/** Somatocarta Y: 2×mesomorphy - (ectomorphy + endomorphy). */
	@Column(precision = 4)
	private Double somatocartaY;

	/** Clinical source for consolidated body fat / muscle values. */
	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private MetodoObtencionComposicionCorporal metodoObtencion;

}
