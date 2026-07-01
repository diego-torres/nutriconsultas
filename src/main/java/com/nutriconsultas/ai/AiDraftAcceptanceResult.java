package com.nutriconsultas.ai;

/**
 * Result of accepting an AI draft and materializing it into catalog data.
 */
public record AiDraftAcceptanceResult(long draftId, AiDraftType draftType, AiDraftStatus status,
		AiDraftCreatedEntityType createdEntityType, long createdEntityId, String summary) {
}
