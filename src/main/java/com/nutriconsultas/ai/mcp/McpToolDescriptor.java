package com.nutriconsultas.ai.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool descriptor for {@code tools/list} (#393).
 */
public record McpToolDescriptor(String name, String title, String description, Map<String, Object> inputSchema,
		boolean readOnly, boolean requiresNutritionistConfirmation) {

	public McpToolDescriptor {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title is required");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("description is required");
		}
		if (inputSchema == null || inputSchema.isEmpty()) {
			throw new IllegalArgumentException("inputSchema is required");
		}
	}

	/**
	 * JSON-friendly map aligned with MCP {@code Tool} shape and project extensions.
	 */
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", name);
		map.put("title", title);
		map.put("description", description);
		map.put("inputSchema", inputSchema);
		if (readOnly) {
			map.put("annotations", Map.of("readOnlyHint", true));
		}
		if (requiresNutritionistConfirmation) {
			map.put("requiresNutritionistConfirmation", true);
		}
		return map;
	}

}
