package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Input for {@code create_menu_draft}.
 */
public record MenuDraftInput(@Nullable String title, @Nullable Double targetKcal, List<IngestaSlotInput> ingestas,
		@Nullable String validationSummary, @Nullable List<String> assumptions, @Nullable List<String> warnings) {
}
