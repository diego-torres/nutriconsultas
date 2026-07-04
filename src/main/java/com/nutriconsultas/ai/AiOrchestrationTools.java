package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;

/**
 * Tool catalog and dispatcher for {@link AiOrchestrationServiceImpl}.
 */
@Component
public class AiOrchestrationTools {

	private final AiOpenAiToolCatalog catalog;

	private final AiOrchestrationToolDispatcher dispatcher;

	private final AiOrchestrationGuardrails guardrails;

	public AiOrchestrationTools(final AiOpenAiToolCatalog catalog, final AiOrchestrationToolDispatcher dispatcher,
			final AiOrchestrationGuardrails guardrails) {
		this.catalog = catalog;
		this.dispatcher = dispatcher;
		this.guardrails = guardrails;
	}

	public AiOpenAiToolCatalog getToolCatalog() {
		return catalog;
	}

	public AiOrchestrationToolDispatcher getToolDispatcher() {
		return dispatcher;
	}

	public AiOrchestrationGuardrails getGuardrails() {
		return guardrails;
	}

}
