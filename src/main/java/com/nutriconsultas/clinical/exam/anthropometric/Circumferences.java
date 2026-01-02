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
public class Circumferences {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double cephalicCircumference;

	@Column(precision = 5)
	private Double neckCircumference;

	@Column(precision = 5)
	private Double midUpperArmCircumferenceRelaxed;

	@Column(precision = 5)
	private Double midUpperArmCircumferenceContracted;

	@Column(precision = 5)
	private Double forearmCircumference;

	@Column(precision = 5)
	private Double wristCircumference;

	@Column(precision = 5)
	private Double mesosternalCircumference;

	@Column(precision = 5)
	private Double umbilicalCircumference;

	@Column(precision = 5)
	private Double waistCircumference;

	@Column(precision = 5)
	private Double hipCircumference;

	@Column(precision = 5)
	private Double thighCircumference;

	@Column(precision = 5)
	private Double midThighCircumference;

	@Column(precision = 5)
	private Double calfCircumference;

	@Column(precision = 5)
	private Double ankleCircumference;

}
