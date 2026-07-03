package com.nutriconsultas.ai;

import java.util.List;

/**
 * Redacted dish context for the AI system prompt.
 */
public record AiPlatilloPromptContext(Long platilloId, String name, String descriptionSummary, Integer energiaKcal,
		int ingredientCount, List<String> ingredientNames, String ingestasSugeridas) {

	public AiPlatilloPromptContext {
		ingredientNames = ingredientNames == null ? List.of() : List.copyOf(ingredientNames);
	}

}
