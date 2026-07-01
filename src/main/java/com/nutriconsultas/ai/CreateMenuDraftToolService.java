package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;

/**
 * AI tool {@code create_menu_draft} — persist a one-day menu draft for nutritionist
 * review.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CreateMenuDraftToolService {

	String TOOL_NAME = "create_menu_draft";

	AiToolResult<AiDraftCreationData> createDraft(@NonNull String nutritionistId, long threadId,
			@NonNull MenuDraftInput input);

}
