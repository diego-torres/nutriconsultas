package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Nutritionist-scoped AI chat thread operations (#384).
 */
public interface AiChatService {

	AiChatThread startThread(String nutritionistId, @Nullable String title, @Nullable Long patientId,
			@Nullable Long clinicId, AiChatPromptContext promptContext);

	AiChatThreadDetail getThread(String nutritionistId, long threadId);

	AiChatDraftList listDrafts(String nutritionistId, long threadId);

	AiOrchestrationResult sendMessage(String nutritionistId, long threadId, String message,
			AiChatPromptContext promptContext);

}
