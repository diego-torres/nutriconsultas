package com.nutriconsultas.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Chat message for OpenAI chat completions (#366).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatMessage(String role, String content, String toolCallId, String name,
		List<OpenAiToolCall> toolCalls) {

	public OpenAiChatMessage {
		if (role == null || role.isBlank()) {
			throw new IllegalArgumentException("role is required");
		}
		if (toolCalls != null && !toolCalls.isEmpty()) {
			toolCalls = List.copyOf(toolCalls);
		}
	}

	public static OpenAiChatMessage system(final String content) {
		return new OpenAiChatMessage("system", content, null, null, null);
	}

	public static OpenAiChatMessage user(final String content) {
		return new OpenAiChatMessage("user", content, null, null, null);
	}

	public static OpenAiChatMessage assistant(final String content) {
		return new OpenAiChatMessage("assistant", content, null, null, null);
	}

	public static OpenAiChatMessage assistantWithToolCalls(final String content, final List<OpenAiToolCall> toolCalls) {
		return new OpenAiChatMessage("assistant", content, null, null, toolCalls);
	}

	public static OpenAiChatMessage tool(final String toolCallId, final String name, final String content) {
		return new OpenAiChatMessage("tool", content, toolCallId, name, null);
	}

}
