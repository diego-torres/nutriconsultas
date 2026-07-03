package com.nutriconsultas.ai;

import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Structured draft preview for nutritionist review UI (#390).
 */
public record AiDraftPreviewView(long draftId, long threadId, AiDraftType draftType, AiDraftStatus status,
		String draftTypeLabel, String reviewLabel, @Nullable String title, String summary, @Nullable Integer portions,
		@Nullable Integer dayCount, @Nullable NutrientSummary nutrients, List<Map<String, String>> ingredients,
		List<Map<String, Object>> mealSlots, List<String> preparationSteps, List<String> assumptions,
		List<String> warnings, @Nullable String validationSummary) {
}
