package com.nutriconsultas.paciente;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalExam {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id")
	@NotNull(message = "El paciente es requerido")
	private Paciente paciente;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull(message = "La fecha y hora son requeridas")
	private Date examDateTime;

	@NotBlank(message = "El título es requerido")
	@Column(nullable = false, length = 200)
	private String title = "Examen Clínico";

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String summaryNotes;

	// VITAL SIGNS
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

	// LIPID PROFILE
	@Column(precision = 5)
	private Double hdl;

	@Column(precision = 5)
	private Double ldl;

	@Column(precision = 5)
	private Double trigliceridos;

	@Column(precision = 5)
	private Double colesterolTotal;

	// BLOOD CHEMISTRY
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

	// LIVER FUNCTION
	@Column(precision = 5)
	private Double alt;

	@Column(precision = 5)
	private Double ast;

	@Column(precision = 5)
	private Double bilirrubina;

	// COMPLETE BLOOD COUNT
	@Column(precision = 5)
	private Double hemoglobina;

	@Column(precision = 5)
	private Double hematocrito;

	@Column(precision = 5)
	private Double leucocitos;

	@Column(precision = 5)
	private Double plaquetas;

	// OTHER TESTS
	@Column(precision = 5)
	private Double vitaminaD;

	@Column(precision = 5)
	private Double vitaminaB12;

	@Column(precision = 5)
	private Double hierro;

	@Column(precision = 5)
	private Double ferritina;

}

