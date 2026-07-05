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

	private final AiAuditLogger auditLogger;

	public AiOrchestrationTools(final AiOpenAiToolCatalog catalog, final AiOrchestrationToolDispatcher dispatcher,
			final AiOrchestrationGuardrails guardrails, final AiAuditLogger auditLogger) {
		this.catalog = catalog;
		this.dispatcher = dispatcher;
		this.guardrails = guardrails;
		this.auditLogger = auditLogger;
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

	public AiAuditLogger getAuditLogger() {
		return auditLogger;
	}

}
