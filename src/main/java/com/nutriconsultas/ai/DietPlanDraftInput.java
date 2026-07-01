package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Input for {@code create_diet_plan_draft}.
 */
public record DietPlanDraftInput(@Nullable String title, @Nullable Integer dayCount, @Nullable Double targetKcalPerDay,
		List<DietPlanDayInput> days, @Nullable String validationSummary, @Nullable List<String> assumptions,
		@Nullable List<String> warnings) {
}
