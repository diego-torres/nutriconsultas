package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;

/**
 * Tool catalog and dispatcher for {@link AiOrchestrationServiceImpl}.
 */
@Component
public class AiOrchestrationTools {

	private final AiOpenAiToolCatalog catalog;

	private final AiOrchestrationToolDispatcher dispatcher;

	public AiOrchestrationTools(final AiOpenAiToolCatalog catalog, final AiOrchestrationToolDispatcher dispatcher) {
		this.catalog = catalog;
		this.dispatcher = dispatcher;
	}

	public AiOpenAiToolCatalog getToolCatalog() {
		return catalog;
	}

	public AiOrchestrationToolDispatcher getToolDispatcher() {
		return dispatcher;
	}

}
