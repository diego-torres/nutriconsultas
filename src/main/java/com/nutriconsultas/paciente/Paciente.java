package com.nutriconsultas.paciente;

import java.time.Instant;
import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.nutriconsultas.paciente.embeddable.PacienteBodySnapshot;
import com.nutriconsultas.paciente.satellite.PacienteEnergyPreferences;
import com.nutriconsultas.paciente.satellite.PacienteMedicalHistory;
import com.nutriconsultas.paciente.validation.ValidPregnancy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "El nombre es requerido")
	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String userId;

	/**
	 * Patient Auth0 {@code sub} claim for mobile JWT identity. Distinct from
	 * {@link #userId} (nutritionist tenant owner). Formal Liquibase changeset tracked in
	 * issue #46.
	 */
	@Column(unique = true, length = 255)
	private String patientAuthSub;

	/**
	 * Apple Sign-In stable subject for mobile patient identity (#504). Distinct from
	 * relay email; used for Apple server notification mapping.
	 */
	@Column(name = "apple_subject", unique = true, length = 255)
	private String appleSubject;

	/**
	 * Apple server notification lifecycle (#506). Never hard-deletes data automatically.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "apple_lifecycle_status", nullable = false, length = 30)
	private ApplePacienteLifecycleStatus appleLifecycleStatus = ApplePacienteLifecycleStatus.NONE;

	/**
	 * Apple Hide My Email relay address from server notifications (#507). Not the primary
	 * identity key — use {@link #appleSubject} and {@link #patientAuthSub}.
	 */
	@Column(name = "apple_relay_email", length = 320)
	private String appleRelayEmail;

	@Column(name = "apple_relay_private_email")
	private Boolean appleRelayPrivateEmail;

	@Column(name = "apple_relay_forwarding_enabled")
	private Boolean appleRelayForwardingEnabled;

	@Column(name = "apple_relay_updated_at")
	private Instant appleRelayUpdatedAt;

	/**
	 * Invite-only onboarding lifecycle (#132). Existing patients default to
	 * {@link PacienteStatus#ACTIVE}.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PacienteStatus status = PacienteStatus.ACTIVE;

	/**
	 * Human/clinical identifier assigned at invite time. Unique when set; not used for
	 * auth.
	 */
	@Column(name = "assigned_id", unique = true, length = 50)
	private String assignedId;

	/**
	 * Optional email pre-fill hint for onboarding UI — never used for auth decisions.
	 */
	@Column(name = "email_hint", length = 100)
	private String emailHint;

	@Column(name = "display_name", length = 100)
	private String displayName;

	/**
	 * Selected avatar key from {@link PacienteAvatarCatalog} (#241). When null, UI uses
	 * gender-based default.
	 */
	@Column(name = "avatar_id", length = 32)
	private String avatarId;

	/**
	 * Custom profile photo extension in S3 ({@code patients/{id}/photo.{ext}}), #529.
	 * When null, UI uses {@link PacienteAvatarCatalog}.
	 */
	@Column(name = "photo_extension", length = 16)
	private String photoExtension;

	public static final String DATE_OF_BIRTH_PATTERN = "dd/MM/yyyy";

	@DateTimeFormat(pattern = DATE_OF_BIRTH_PATTERN)
	@Temporal(TemporalType.DATE)
	@NotNull(message = "La fecha de nacimiento es requerida")
	@Column(nullable = false)
	private Date dob;

	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date registro = new Date();

	@Column(length = 100)
	private String email;

	@Column(length = 25)
	private String phone;

	@NotBlank(message = "El género es requerido")
	@Column(nullable = false, length = 1)
	private String gender;

	@Column(length = 100)
	private String responsibleName;

	private String parentesco;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "peso", column = @Column(name = "peso")),
			@AttributeOverride(name = "estatura", column = @Column(name = "estatura")),
			@AttributeOverride(name = "imc", column = @Column(name = "imc")),
			@AttributeOverride(name = "bmr", column = @Column(name = "bmr")),
			@AttributeOverride(name = "getKcal", column = @Column(name = "get_kcal")),
			@AttributeOverride(name = "nivelPeso", column = @Column(name = "nivel_peso")),
			@AttributeOverride(name = "tefKcal", column = @Column(name = "tef_kcal")),
			@AttributeOverride(name = "totalAdjustedKcal", column = @Column(name = "total_adjusted_kcal")),
			@AttributeOverride(name = "stressKcal", column = @Column(name = "stress_kcal")),
			@AttributeOverride(name = "finalTotalKcal", column = @Column(name = "final_total_kcal")) })
	@Delegate
	private PacienteBodySnapshot bodySnapshot = new PacienteBodySnapshot();

	@LazyToOne(LazyToOneOption.NO_PROXY)
	@OneToOne(mappedBy = "paciente", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Delegate(excludes = PacienteEnergyPreferencesDelegateExcludes.class)
	private PacienteEnergyPreferences energyPreferences = new PacienteEnergyPreferences();

	@LazyToOne(LazyToOneOption.NO_PROXY)
	@OneToOne(mappedBy = "paciente", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Delegate(excludes = PacienteMedicalHistoryDelegateExcludes.class)
	private PacienteMedicalHistory medicalHistory = new PacienteMedicalHistory();

	// ESTADO DE EMBARAZO (solo para mujeres entre 12-50 años)
	@ValidPregnancy
	private Boolean pregnancy = false;

	@PostLoad
	@PrePersist
	@PreUpdate
	void ensureEmbeddedRows() {
		if (bodySnapshot == null) {
			bodySnapshot = new PacienteBodySnapshot();
		}
		if (energyPreferences == null) {
			energyPreferences = new PacienteEnergyPreferences();
		}
		if (medicalHistory == null) {
			medicalHistory = new PacienteMedicalHistory();
		}
		energyPreferences.setPaciente(this);
		medicalHistory.setPaciente(this);
	}

	private interface PacienteEnergyPreferencesDelegateExcludes {

		Long getId();

		void setId(Long id);

		Paciente getPaciente();

		void setPaciente(Paciente paciente);

	}

	private interface PacienteMedicalHistoryDelegateExcludes {

		Long getId();

		void setId(Long id);

		Paciente getPaciente();

		void setPaciente(Paciente paciente);

	}

}
