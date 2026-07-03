package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Optional entity identifiers sent with AI chat requests to inject page context (#389
 * widget).
 */
public record AiChatPromptContext(@Nullable Long patientId, @Nullable Long dietaId, @Nullable Long platilloId) {

	public static AiChatPromptContext empty() {
		return new AiChatPromptContext(null, null, null);
	}

}
