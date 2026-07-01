package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * JSON payload persisted for a diet plan draft ({@code create_diet_plan_draft}).
 */
public record DietPlanDraftPayload(@Nullable String title, int dayCount, @Nullable Double targetKcalPerDay,
		List<DietPlanDayPayload> days, NutrientSummary weeklyAverageNutrients, @Nullable String validationSummary,
		@Nullable List<String> assumptions, @Nullable List<String> warnings, String label) {
}
