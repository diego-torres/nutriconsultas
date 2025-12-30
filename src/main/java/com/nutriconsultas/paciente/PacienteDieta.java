package com.nutriconsultas.paciente;

import java.util.Date;

import com.nutriconsultas.dieta.Dieta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDieta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id")
	@NotNull(message = "El paciente es requerido")
	private Paciente paciente;

	@ManyToOne(optional = false)
	@JoinColumn(name = "dieta_id")
	@NotNull(message = "La dieta es requerida")
	private Dieta dieta;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	@NotNull(message = "La fecha de inicio es requerida")
	@Column(nullable = false)
	private Date startDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	@Column(nullable = true)
	private Date endDate;

	@Column(nullable = false)
	@NotNull(message = "El estado es requerido")
	private PacienteDietaStatus status = PacienteDietaStatus.ACTIVE;

	@Column(columnDefinition = "TEXT")
	private String notes;

}
