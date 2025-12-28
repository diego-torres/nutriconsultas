package com.nutriconsultas.consulta;

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
public class Consulta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id")
	private Paciente paciente;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	private Date fechaConsulta;

	@Column(precision = 5)
	@NotNull
	@Min(10)
	@Max(200)
	private Double peso;

	@Column(precision = 3)
	@NotNull
	@Max(3)
	@DecimalMin(value = "0.5")
	private Double estatura;

	@Column(precision = 3)
	private Double imc;

	private NivelPeso nivelPeso;

	private Integer sistolica, diastolica, pulso, indiceGlucemico;

	@Column(precision = 5)
	private Double spo2;

	@Column(precision = 5)
	private Double temperatura;

	@Column(columnDefinition = "TEXT")
	private String notasInterconsulta;

}
