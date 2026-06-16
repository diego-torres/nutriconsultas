package com.nutriconsultas.subscription;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clinic_member",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_clinic_member_clinic_user", columnNames = { "clinic_id", "user_id" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClinicMemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "membership_status", nullable = false, length = 20)
	private MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

	@Column(name = "invited_by", length = 255)
	private String invitedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
