package com.nutriconsultas.ai;

/**
 * Pre-orchestration scope decision that short-circuits the tool loop (#448).
 */
public record AiRequestScopeClassifierOutcome(AiRequestScopeDecision decision, String assistantMessage,
		AiRequestScopeRequestedUnits requestedUnits) {

}
