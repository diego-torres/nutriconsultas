package com.nutriconsultas.subscription;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clinic")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clinic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "director_user_id", nullable = false, length = 255)
	private String directorUserId;

	@OneToOne(optional = false)
	@JoinColumn(name = "subscription_id", nullable = false, unique = true)
	private Subscription subscription;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
