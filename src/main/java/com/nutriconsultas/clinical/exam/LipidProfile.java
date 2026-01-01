package com.nutriconsultas.clinical.exam;

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
public class LipidProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double hdl;

	@Column(precision = 5)
	private Double ldl;

	@Column(precision = 5)
	private Double trigliceridos;

	@Column(precision = 5)
	private Double colesterolTotal;

}

