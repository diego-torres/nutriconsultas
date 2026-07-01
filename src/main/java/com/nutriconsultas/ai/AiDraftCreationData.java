package com.nutriconsultas.ai;

/**
 * Common success payload for draft-creation tools.
 */
public record AiDraftCreationData(long draftId, AiDraftType draftType, AiDraftStatus status, String summary) {
}
