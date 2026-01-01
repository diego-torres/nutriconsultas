package com.nutriconsultas.clinical.exam;

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
public class VitalSigns {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(precision = 5)
	@Min(10)
	@Max(200)
	private Double peso;

	@Column(precision = 3)
	@Max(3)
	@DecimalMin(value = "0.5")
	private Double estatura;

	@Column(precision = 3)
	private Double imc;

	@Column(precision = 3)
	private Double indiceGrasaCorporal;

	private NivelPeso nivelPeso;

	private Integer sistolica;

	private Integer diastolica;

	private Integer pulso;

	private Integer indiceGlucemico;

	@Column(precision = 5)
	private Double spo2;

	@Column(precision = 5)
	private Double temperatura;

}

