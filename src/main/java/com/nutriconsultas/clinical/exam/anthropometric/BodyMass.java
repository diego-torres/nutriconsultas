package com.nutriconsultas.clinical.exam.anthropometric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nutriconsultas.paciente.NivelPeso;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyMass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	@Min(10)
	@Max(200)
	private Double weight;

	@Column(precision = 3)
	@Max(3)
	@DecimalMin(value = "0.5")
	private Double height;

	@Column(precision = 3)
	private Double bodyMassValue;

	@Column(precision = 3)
	private Double imc;

	private NivelPeso nivelPeso;

}
