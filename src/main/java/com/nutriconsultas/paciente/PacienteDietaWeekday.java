package com.nutriconsultas.paciente;

import com.nutriconsultas.dieta.Dieta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "paciente_dieta_weekday",
		uniqueConstraints = { @UniqueConstraint(name = "uk_paciente_dieta_weekday_day",
				columnNames = { "paciente_dieta_id", "day_of_week" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDietaWeekday {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_dieta_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PacienteDieta pacienteDieta;

	/** ISO-8601 day: 1 = Monday … 7 = Sunday (matches booking working hours). */
	@Column(name = "day_of_week", nullable = false)
	private Integer dayOfWeek;

	@ManyToOne(optional = false)
	@JoinColumn(name = "dieta_id")
	private Dieta dieta;

}
