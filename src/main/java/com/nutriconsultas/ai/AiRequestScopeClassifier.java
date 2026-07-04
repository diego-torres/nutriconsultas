package com.nutriconsultas.ai;

import java.util.Optional;

/**
 * Optional LLM pre-flight scope evaluation before the main orchestration tool loop (#448).
 */
@FunctionalInterface
public interface AiRequestScopeClassifier {

	/**
	 * @return empty when {@link AiRequestScopeDecision#ALLOW}, disabled, or classifier fails
	 *         open; present with assistant message for REFUSE/CLARIFY
	 */
	Optional<AiRequestScopeClassifierOutcome> evaluate(String sanitizedUserMessage);

}
