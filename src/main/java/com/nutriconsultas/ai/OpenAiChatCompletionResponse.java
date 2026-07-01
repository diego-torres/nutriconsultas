package com.nutriconsultas.ai;

import java.util.List;

/**
 * Normalized OpenAI chat completion response (#366).
 */
public record OpenAiChatCompletionResponse(String id, String role, String content, List<OpenAiToolCall> toolCalls,
		String finishReason, OpenAiTokenUsage usage) {

	public OpenAiChatCompletionResponse {
		toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
	}

	public boolean hasToolCalls() {
		return !toolCalls.isEmpty();
	}

}
