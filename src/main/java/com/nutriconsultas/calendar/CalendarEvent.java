package com.nutriconsultas.calendar;

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
public class CalendarEvent {

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
	private Date eventDateTime;

	@NotBlank(message = "El título es requerido")
	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	@NotNull(message = "La duración es requerida")
	private Integer durationMinutes;

	@Column(nullable = false)
	@NotNull(message = "El estado es requerido")
	private EventStatus status = EventStatus.SCHEDULED;

	@Column(columnDefinition = "TEXT")
	private String summaryNotes;

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
