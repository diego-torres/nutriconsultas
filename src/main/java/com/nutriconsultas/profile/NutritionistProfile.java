package com.nutriconsultas.profile;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a nutritionist's professional profile.
 *
 * <p>
 * Keyed by the Auth0 {@code sub} claim (same pattern as {@code Paciente.userId}). Stores
 * regulated professional identifiers and branding assets used in PDF exports.
 *
 * <p>
 * <strong>Privacy:</strong> Never log {@code cedulaProfesional} or other personal fields
 * in plain text. Use
 * {@link com.nutriconsultas.util.LogRedaction#redactNutritionistProfile} when logging
 * profile-related operations.
 */
@Entity
@Table(name = "nutritionist_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Auth0 subject identifier — unique per nutritionist tenant.
	 */
	@Column(nullable = false, unique = true, length = 255)
	private String userId;

	@Column(nullable = false, unique = true, length = 36)
	private String publicBookingId;

	/**
	 * Optional display name override. Falls back to the OIDC {@code name} claim when
	 * blank.
	 */
	@Column(length = 100)
	private String displayName;

	/**
	 * Mexican cédula profesional identifier. Format is alphanumeric, up to 10 chars (e.g.
	 * "12345678").
	 */
	@Column(length = 20)
	private String cedulaProfesional;

	/**
	 * File extension of the stored logo asset in S3 (e.g. "png", "jpg"). Null if no logo
	 * has been uploaded.
	 */
	@Column(length = 10)
	private String logoExtension;

	/**
	 * Record creation timestamp.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, updatable = false)
	private Date registro = new Date();

}
