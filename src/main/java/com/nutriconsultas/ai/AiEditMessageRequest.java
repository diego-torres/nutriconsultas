package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Request body for edit-and-resubmit AI chat endpoints (#437).
 */
public record AiEditMessageRequest(long threadId, long messageId, String message, @Nullable Long patientId,
		@Nullable Long dietaId, @Nullable Long platilloId) {

}
