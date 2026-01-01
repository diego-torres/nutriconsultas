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
public class BloodChemistry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	private Double glucosa;

	@Column(precision = 5)
	private Double hba1c;

	@Column(precision = 5)
	private Double creatinina;

	@Column(precision = 5)
	private Double urea;

	@Column(precision = 5)
	private Double bun;

}

