package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Outcome of processing one nutritionist chat turn (#385).
 */
public record AiOrchestrationResult(long threadId, AiChatMessage assistantMessage, int toolCallsExecuted,
		@Nullable OpenAiTokenUsage tokenUsage) {
}
