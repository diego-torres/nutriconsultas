package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Single day inside a persisted diet plan draft payload.
 */
public record DietPlanDayPayload(int dayIndex, @Nullable String label, List<IngestaSlotInput> ingestas,
		NutrientSummary nutrientsTotal) {
}
