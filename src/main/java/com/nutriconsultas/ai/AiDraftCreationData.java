package com.nutriconsultas.ai;

/**
 * Common success payload for draft-creation tools.
 */
public record AiDraftCreationData(long draftId, AiDraftType draftType, AiDraftStatus status, String summary,
		String previewPath, Long pacienteId) {

	public static String buildPreviewPath(final long threadId, final long draftId) {
		return "/admin/ai?threadId=" + threadId + "&draftId=" + draftId;
	}

}
