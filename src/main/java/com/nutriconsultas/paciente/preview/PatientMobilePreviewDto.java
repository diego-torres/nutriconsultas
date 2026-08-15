package com.nutriconsultas.paciente.preview;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;

/**
 * Aggregated patient mobile-app preview for nutritionist web.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientMobilePreviewDto(String firstName, String nutritionistDisplayName, String avatarUrl,
		PatientProgressSnapshotDto progress, DietPlanSummaryDto activePlan, DietPlanDetailDto activePlanDetail,
		List<DietPlanSummaryDto> plans, VisitSummaryDto nextVisit) {
}
