package com.nutriconsultas.ai;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Thread detail for chat REST responses (#384).
 */
public record AiChatThreadDetail(long threadId, String title, @Nullable Long patientId, @Nullable Long clinicId,
		Instant createdAt, Instant updatedAt, List<AiChatMessageView> messages) {
}
