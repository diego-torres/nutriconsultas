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
public class Diameters {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double biacromialDiameter;

	@Column(precision = 5)
	private Double biiliocrestalDiameter;

	@Column(precision = 5)
	private Double footLength;

	@Column(precision = 5)
	private Double transverseThoraxDiameter;

	@Column(precision = 5)
	private Double anteroposteriorThoraxDiameter;

	@Column(precision = 5)
	private Double humerusDiameter;

	@Column(precision = 5)
	private Double biestiloidWristDiameter;

	@Column(precision = 5)
	private Double femurDiameter;

	@Column(precision = 5)
	private Double bimaleolarDiameter;

	@Column(precision = 5)
	private Double transverseFootDiameter;

	@Column(precision = 5)
	private Double handLength;

	@Column(precision = 5)
	private Double transverseHandDiameter;

}
