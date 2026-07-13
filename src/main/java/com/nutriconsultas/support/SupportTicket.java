package com.nutriconsultas.support;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupportTicketStatus status;

	@Column(name = "admin_notes", length = 2000)
	private String adminNotes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	@PrePersist
	void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (status == null) {
			status = SupportTicketStatus.OPEN;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
