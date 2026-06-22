package com.nutriconsultas.contact;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private boolean readByAdmin;

	@Column(length = 64)
	private String planRoleSlug;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
