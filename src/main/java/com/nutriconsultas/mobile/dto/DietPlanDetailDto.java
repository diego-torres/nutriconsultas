package com.nutriconsultas.mobile.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaStatus;

/**
 * Full assigned diet plan with meal tree for {@code GET
 * /rest/mobile/patient/diet-plans/{assignmentId}} (#94).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlanDetailDto(Long assignmentId, PacienteDietaStatus status, LocalDate startDate, LocalDate endDate,
		String notes, String dietaName, Integer totalKcal, Double totalProteina, Double totalGrasas,
		Double totalCarbohidratos, List<DietIngestaDto> ingestas) {

	public static DietPlanDetailDto fromEntity(final PacienteDieta assignment) {
		if (assignment == null) {
			return null;
		}
		final Dieta dieta = assignment.getDieta();
		final List<DietIngestaDto> ingestas = dieta != null && dieta.getIngestas() != null ? dieta.getIngestas()
			.stream()
			.sorted(Comparator.comparingLong(Ingesta::getId))
			.map(DietIngestaDto::fromEntity)
			.toList() : List.of();
		return new DietPlanDetailDto(assignment.getId(), assignment.getStatus(), toLocalDate(assignment.getStartDate()),
				toLocalDate(assignment.getEndDate()), assignment.getNotes(), dieta != null ? dieta.getNombre() : null,
				dieta != null ? dieta.getEnergia() : null, dieta != null ? dieta.getProteina() : null,
				dieta != null ? dieta.getLipidos() : null, dieta != null ? dieta.getHidratosDeCarbono() : null,
				ingestas);
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
