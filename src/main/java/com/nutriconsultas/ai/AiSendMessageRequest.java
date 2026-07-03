package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Request body for {@code POST /rest/nutritionist/ai/chat/message} (#384).
 */
public record AiSendMessageRequest(long threadId, String message, @Nullable Long patientId, @Nullable Long dietaId,
		@Nullable Long platilloId) {

	public AiSendMessageRequest(final long threadId, final String message) {
		this(threadId, message, null, null, null);
	}

}
