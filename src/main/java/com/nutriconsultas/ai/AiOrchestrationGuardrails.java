package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;

/**
 * Defense-in-depth guardrails for orchestration: tool allowlist, tool-result
 * sanitization, assistant output validation (#441).
 */
@Component
public final class AiOrchestrationGuardrails {

	private final AiToolAllowlist toolAllowlist;

	private final AiToolResultSanitizer toolResultSanitizer;

	private final AiAssistantOutputValidator assistantOutputValidator;

	public AiOrchestrationGuardrails(final AiToolAllowlist toolAllowlist,
			final AiToolResultSanitizer toolResultSanitizer,
			final AiAssistantOutputValidator assistantOutputValidator) {
		this.toolAllowlist = toolAllowlist;
		this.toolResultSanitizer = toolResultSanitizer;
		this.assistantOutputValidator = assistantOutputValidator;
	}

	public boolean isToolAllowed(final String toolName) {
		return toolAllowlist.isAllowed(toolName);
	}

	public String sanitizeToolResult(final String toolName, final String rawJson) {
		return toolResultSanitizer.sanitizeForModel(toolName, rawJson);
	}

	public String validateAssistantOutput(final String assistantContent) {
		return assistantOutputValidator.validateAndSanitize(assistantContent);
	}

}
