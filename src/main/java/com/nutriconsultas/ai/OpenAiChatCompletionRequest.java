package com.nutriconsultas.ai;

import java.util.List;

/**
 * Request to {@link OpenAiClientService#chatCompletion(OpenAiChatCompletionRequest)}.
 */
public record OpenAiChatCompletionRequest(List<OpenAiChatMessage> messages, List<OpenAiToolDefinition> tools) {

	public OpenAiChatCompletionRequest {
		if (messages == null || messages.isEmpty()) {
			throw new IllegalArgumentException("messages must not be empty");
		}
		messages = List.copyOf(messages);
		tools = tools == null ? List.of() : List.copyOf(tools);
	}

}
