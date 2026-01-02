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
public class Skinfolds {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double subscapularSkinfold;

	@Column(precision = 5)
	private Double tricepsSkinfold;

	@Column(precision = 5)
	private Double bicepsSkinfold;

	@Column(precision = 5)
	private Double iliacCrestSkinfold;

	@Column(precision = 5)
	private Double supraespinalSkinfold;

	@Column(precision = 5)
	private Double abdominalSkinfold;

	@Column(precision = 5)
	private Double frontalThighSkinfold;

	@Column(precision = 5)
	private Double medialCalfSkinfold;

	@Column(precision = 5)
	private Double medialAxillarySkinfold;

	@Column(precision = 5)
	private Double pectoralSkinfold;

}
