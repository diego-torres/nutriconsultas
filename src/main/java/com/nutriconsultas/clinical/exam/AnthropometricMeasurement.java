package com.nutriconsultas.clinical.exam;

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

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropometricMeasurement {

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
	private Date measurementDateTime;

	@NotBlank(message = "El título es requerido")
	@Column(nullable = false, length = 200)
	private String title = "Medición Antropométrica";

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String notes;

	// Anthropometric measurements
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

	// Additional body measurements
	@Column(precision = 5)
	private Double cintura;

	@Column(precision = 5)
	private Double cadera;

	@Column(precision = 5)
	private Double cuello;

	@Column(precision = 5)
	private Double brazo;

	@Column(precision = 5)
	private Double muslo;

	@Column(precision = 3)
	private Double porcentajeGrasaCorporal;

	@Column(precision = 3)
	private Double porcentajeMasaMuscular;

}
