package com.nutriconsultas.ai;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Server-side allowlist of OpenAI tool names registered in {@link AiOpenAiToolCatalog}
 * (#441).
 */
@Component
public final class AiToolAllowlist {

	static final String REJECTION_MESSAGE = "Herramienta no permitida.";

	private final Set<String> registeredToolNames;

	public AiToolAllowlist(final AiOpenAiToolCatalog catalog) {
		this.registeredToolNames = catalog.definitions()
			.stream()
			.map(OpenAiToolDefinition::name)
			.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isAllowed(final String toolName) {
		return toolName != null && registeredToolNames.contains(toolName);
	}

	public Set<String> allowedToolNames() {
		return registeredToolNames;
	}

}
