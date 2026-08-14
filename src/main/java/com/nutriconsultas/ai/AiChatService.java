package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Nutritionist-scoped AI chat thread operations (#384).
 */
public interface AiChatService {

	AiChatThread startThread(String nutritionistId, @Nullable String title, @Nullable Long patientId,
			@Nullable Long clinicId, AiChatPromptContext promptContext);

	AiChatThreadDetail getThread(String nutritionistId, long threadId);

	AiChatDraftList listDrafts(String nutritionistId, long threadId);

	/**
	 * Pending ({@link AiDraftStatus#DRAFT}) drafts owned by the nutritionist across all threads.
	 */
	List<AiChatDraftSummary> listPendingDrafts(String nutritionistId);

	AiOrchestrationResult sendMessage(String nutritionistId, long threadId, String message,
			AiChatPromptContext promptContext);

	void streamMessage(String nutritionistId, AiSendMessageRequest request, SseEmitter emitter);

	AiEditResubmitResult editAndResubmitMessage(String nutritionistId, AiEditMessageRequest request);

	void streamEditMessage(String nutritionistId, AiEditMessageRequest request, SseEmitter emitter);

}
