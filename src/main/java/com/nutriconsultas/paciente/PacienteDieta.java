package com.nutriconsultas.paciente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nutriconsultas.dieta.Dieta;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

	@ManyToOne(optional = true)
	@JoinColumn(name = "dieta_id")
	private Dieta dieta;

	@Enumerated(EnumType.STRING)
	@Column(name = "assignment_type", nullable = false, length = 16)
	@NotNull(message = "El tipo de asignación es requerido")
	private PacienteDietaAssignmentType assignmentType = PacienteDietaAssignmentType.DATE_RANGE;

	@OneToMany(mappedBy = "pacienteDieta", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PacienteDietaWeekday> weekdaySlots = new ArrayList<>();

	public boolean isWeeklyAssignment() {
		return assignmentType == PacienteDietaAssignmentType.WEEKLY;
	}

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
