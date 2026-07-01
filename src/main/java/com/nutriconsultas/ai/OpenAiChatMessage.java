package com.nutriconsultas.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Chat message for OpenAI chat completions (#366).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatMessage(String role, String content, String toolCallId, String name) {

	public OpenAiChatMessage {
		if (role == null || role.isBlank()) {
			throw new IllegalArgumentException("role is required");
		}
	}

	public static OpenAiChatMessage system(final String content) {
		return new OpenAiChatMessage("system", content, null, null);
	}

	public static OpenAiChatMessage user(final String content) {
		return new OpenAiChatMessage("user", content, null, null);
	}

	public static OpenAiChatMessage assistant(final String content) {
		return new OpenAiChatMessage("assistant", content, null, null);
	}

	public static OpenAiChatMessage tool(final String toolCallId, final String name, final String content) {
		return new OpenAiChatMessage("tool", content, toolCallId, name);
	}

}
