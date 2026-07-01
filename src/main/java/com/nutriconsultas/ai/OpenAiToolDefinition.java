package com.nutriconsultas.ai;

import java.util.Map;

/**
 * Function tool definition for OpenAI tool calling (#363, #366).
 */
public record OpenAiToolDefinition(String name, String description, Map<String, Object> parameters) {

	public OpenAiToolDefinition {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("description is required");
		}
	}

}
