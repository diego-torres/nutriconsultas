package com.nutriconsultas.mobile.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaStatus;

/**
 * Assigned diet plan summary for {@code GET /rest/mobile/patient/diet-plans} (#93).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlanSummaryDto(Long assignmentId, PacienteDietaStatus status, LocalDate startDate, LocalDate endDate,
		String notes, String dietaName, Integer totalKcal, Double totalProteina, Double totalGrasas,
		Double totalCarbohidratos) {

	public static DietPlanSummaryDto fromEntity(final PacienteDieta assignment) {
		if (assignment == null) {
			return null;
		}
		if (assignment.isWeeklyAssignment()) {
			return new DietPlanSummaryDto(assignment.getId(), assignment.getStatus(),
					toLocalDate(assignment.getStartDate()), toLocalDate(assignment.getEndDate()), assignment.getNotes(),
					"Plan semanal", null, null, null, null);
		}
		final Dieta dieta = assignment.getDieta();
		return new DietPlanSummaryDto(assignment.getId(), assignment.getStatus(),
				toLocalDate(assignment.getStartDate()), toLocalDate(assignment.getEndDate()), assignment.getNotes(),
				dieta != null ? dieta.getNombre() : null, dieta != null ? dieta.getEnergia() : null,
				dieta != null ? dieta.getProteina() : null, dieta != null ? dieta.getLipidos() : null,
				dieta != null ? dieta.getHidratosDeCarbono() : null);
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
