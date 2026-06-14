package com.nutriconsultas.message;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nutriconsultas.paciente.Paciente;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id", nullable = false)
	private Paciente paciente;

	@Column(nullable = false, length = 255)
	private String nutritionistUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageSenderRole senderRole;

	@Column(nullable = false, length = 2000)
	private String body;

	@Column(nullable = false)
	private Instant sentAt;

	@Column(nullable = false)
	private boolean readByPatient;

	@PrePersist
	void onCreate() {
		if (sentAt == null) {
			sentAt = Instant.now();
		}
	}

}
