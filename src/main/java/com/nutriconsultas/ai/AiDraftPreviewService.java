package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;

/**
 * Builds nutritionist-facing draft previews (#390).
 */
@FunctionalInterface
public interface AiDraftPreviewService {

	AiDraftPreviewView getPreview(long draftId, @NonNull String nutritionistId);

}
