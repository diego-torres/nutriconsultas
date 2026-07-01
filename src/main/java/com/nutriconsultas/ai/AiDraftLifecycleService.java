package com.nutriconsultas.ai;

/**
 * Nutritionist-scoped lifecycle for AI-generated drafts (#371).
 */
public interface AiDraftLifecycleService {

	AiGeneratedDraft createDraft(Long threadId, String nutritionistId, AiDraftType draftType, String jsonPayload);

	AiGeneratedDraft acceptDraft(Long draftId, String nutritionistId);

	AiGeneratedDraft discardDraft(Long draftId, String nutritionistId);

}
