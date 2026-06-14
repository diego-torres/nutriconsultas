package com.nutriconsultas.paciente.metrics;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

/**
 * Centralized body metric history for a patient (IMC, grasa corporal, peso/talla).
 *
 * <p>
 * Aggregates measurements captured in consultations, anthropometrics, and clinical exams
 * so charts and profile summaries use a single timeline.
 */
@Entity
@Table(name = "body_metric_record",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_body_metric_source", columnNames = { "source", "source_id" }) },
		indexes = { @Index(name = "idx_body_metric_paciente_date", columnList = "paciente_id, recorded_at") })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyMetricRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id", nullable = false)
	private Paciente paciente;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false)
	private Date recordedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BodyMetricSource source;

	@Column(name = "source_id", nullable = false)
	private Long sourceId;

	@Column(precision = 5)
	private Double weight;

	@Column(precision = 3)
	private Double height;

	@Column(precision = 3)
	private Double imc;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private NivelPeso nivelPeso;

	@Column(precision = 3)
	private Double bodyFatIndex;

	@Column(precision = 3)
	private Double bodyFatPercentage;

	@Column(precision = 7)
	private Double bmr;

	@Column(precision = 7)
	private Double getKcal;

	@Column(precision = 7)
	private Double tefKcal;

	@Column(precision = 7)
	private Double totalAdjustedKcal;

}
