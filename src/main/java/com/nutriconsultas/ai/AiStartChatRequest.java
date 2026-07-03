package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Request body for {@code POST /rest/nutritionist/ai/chat/start} (#384).
 */
public record AiStartChatRequest(@Nullable String title, @Nullable Long patientId, @Nullable Long clinicId,
		@Nullable Long dietaId, @Nullable Long platilloId) {
}
