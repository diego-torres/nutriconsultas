package com.nutriconsultas.ai;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Neutralizes indirect prompt-injection patterns in tool JSON returned to the model
 * (#441).
 */
@Component
@Slf4j
public final class AiToolResultSanitizer {

	private static final int MAX_TOOL_RESULT_CHARS = 12_000;

	private static final String FILTERED_PLACEHOLDER = "[contenido filtrado por seguridad]";

	private static final Pattern SYSTEM_ROLE_JSON = Pattern.compile("\"role\"\\s*:\\s*\"system\"",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern INSTRUCTIONS_JSON = Pattern
		.compile("\"(?:instructions|system_prompt|developer_message)\"\\s*:", Pattern.CASE_INSENSITIVE);

	public String sanitizeForModel(final String toolName, final String resultJson) {
		if (resultJson == null || resultJson.isBlank()) {
			return AiPromptDelimiters.wrapToolResult(toolName, "{}");
		}
		String sanitized = neutralizeThreats(resultJson);
		sanitized = stripSuspiciousJsonKeys(sanitized);
		sanitized = truncate(sanitized);
		if (log.isDebugEnabled() && !sanitized.equals(resultJson)) {
			log.debug("AI tool result sanitized tool={} originalLength={} sanitizedLength={}", toolName,
					resultJson.length(), sanitized.length());
		}
		return AiPromptDelimiters.wrapToolResult(toolName, sanitized);
	}

	private static String neutralizeThreats(final String content) {
		if (AiPromptThreatDetector.detect(content).isPresent()) {
			return FILTERED_PLACEHOLDER;
		}
		return content;
	}

	private static String stripSuspiciousJsonKeys(final String content) {
		if (SYSTEM_ROLE_JSON.matcher(content).find() || INSTRUCTIONS_JSON.matcher(content).find()) {
			return FILTERED_PLACEHOLDER;
		}
		return content;
	}

	private static String truncate(final String content) {
		if (content.length() <= MAX_TOOL_RESULT_CHARS) {
			return content;
		}
		return content.substring(0, MAX_TOOL_RESULT_CHARS) + "...";
	}

}
