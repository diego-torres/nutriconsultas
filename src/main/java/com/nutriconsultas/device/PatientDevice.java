package com.nutriconsultas.device;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nutriconsultas.paciente.Paciente;

@Entity
@Table(name = "patient_device")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientDevice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id", nullable = false)
	private Paciente paciente;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PatientDevicePlatform platform;

	@Column(nullable = false, length = 512, unique = true)
	private String token;

	@Column(name = "app_version", length = 50)
	private String appVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@PrePersist
	void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (lastSeenAt == null) {
			lastSeenAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
