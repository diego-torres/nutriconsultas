package com.nutriconsultas.ai;

import java.time.Instant;

/**
 * Draft summary for chat REST responses (#384).
 */
public record AiChatDraftSummary(long draftId, AiDraftType draftType, AiDraftStatus status, String summary,
		Instant createdAt) {
}
