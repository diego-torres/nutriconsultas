package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * JSON payload persisted for a menu draft ({@code create_menu_draft}).
 */
public record MenuDraftPayload(@Nullable String title, @Nullable Double targetKcal, List<IngestaSlotInput> ingestas,
		NutrientSummary nutrientsTotal, @Nullable String validationSummary, @Nullable List<String> assumptions,
		@Nullable List<String> warnings, String label) {
}
