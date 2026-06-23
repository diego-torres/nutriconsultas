package com.nutriconsultas.mobile.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.mobile.PatientOnboardingCompleteness;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Patient onboarding bootstrap profile for {@code GET /rest/mobile/patient/me} (#138).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Patient onboarding profile")
public record PatientOnboardingProfileDto(@Schema(description = "Paciente id", example = "1001") Long pacienteId,
		@Schema(description = "Lifecycle status", example = "ONBOARDING") PacienteStatus status,
		@Schema(description = "Legal/clinical name", example = "María López") String name,
		@Schema(description = "Display name for mobile UI", example = "María") String displayName,
		@Schema(description = "Date of birth (ISO-8601 date)", example = "1990-05-15") LocalDate dob,
		@Schema(description = "Gender (M/F)", example = "F") String gender,
		@Schema(description = "Contact email", example = "maria@example.com") String email,
		@Schema(description = "Contact phone", example = "+525512345678") String phone,
		@Schema(description = "Selected avatar id", example = "avatar_6") String avatarId,
		@Schema(description = "Clinic-facing identifier", example = "P-CLINIC-001") String assignedId,
		@Schema(description = "True when all required onboarding fields are present",
				example = "false") boolean profileComplete,
		@Schema(description = "Primary active diet assignment, when present") AssignedDietPlanReferenceDto assignedDietPlan) {

	public static PatientOnboardingProfileDto fromEntity(final Paciente paciente,
			final AssignedDietPlanReferenceDto assignedDietPlan) {
		return new PatientOnboardingProfileDto(paciente.getId(), paciente.getStatus(), paciente.getName(),
				paciente.getDisplayName(), toLocalDate(paciente.getDob()), paciente.getGender(), paciente.getEmail(),
				paciente.getPhone(), paciente.getAvatarId(), paciente.getAssignedId(),
				PatientOnboardingCompleteness.isComplete(paciente), assignedDietPlan);
	}

	public static AssignedDietPlanReferenceDto toAssignedDietPlanReference(final PacienteDieta assignment) {
		if (assignment == null) {
			return null;
		}
		final String dietaName = assignment.getDieta() != null ? assignment.getDieta().getNombre() : null;
		return new AssignedDietPlanReferenceDto(assignment.getId(), assignment.getStatus(), dietaName);
	}

	public static AssignedDietPlanReferenceDto resolvePrimaryAssignment(final List<PacienteDieta> assignments) {
		return assignments.stream()
			.filter(assignment -> assignment.getStatus() == PacienteDietaStatus.ACTIVE)
			.findFirst()
			.map(PatientOnboardingProfileDto::toAssignedDietPlanReference)
			.orElse(null);
	}

	private static LocalDate toLocalDate(final Date date) {
		if (date == null) {
			return null;
		}
		if (date instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

}
