package com.nutriconsultas.ai;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Deterministic scope guard plus optional LLM classifier (#447, #448).
 */
@Component
public class AiRequestScopePipeline {

	/**
	 * Short-circuit reply before main orchestration.
	 */
	public record ScopeShortCircuit(String assistantMessage, String sourceLabel, Object sourceDetail) {
	}

	private final AiRequestScopeGuard deterministicGuard;

	private final AiRequestScopeClassifier classifier;

	public AiRequestScopePipeline(final AiRequestScopeGuard deterministicGuard,
			final AiRequestScopeClassifier classifier) {
		this.deterministicGuard = deterministicGuard;
		this.classifier = classifier;
	}

	public Optional<ScopeShortCircuit> evaluate(final String sanitizedUserMessage) {
		final Optional<AiRequestScopeViolation> violation = deterministicGuard.evaluate(sanitizedUserMessage);
		if (violation.isPresent()) {
			final AiRequestScopeViolation scopeViolation = violation.get();
			return Optional
				.of(new ScopeShortCircuit(scopeViolation.refusalMessage(), "scope refusal", scopeViolation.kind()));
		}
		final Optional<AiRequestScopeClassifierOutcome> classifierOutcome = classifier.evaluate(sanitizedUserMessage);
		if (classifierOutcome.isPresent()) {
			final AiRequestScopeClassifierOutcome outcome = classifierOutcome.get();
			return Optional.of(new ScopeShortCircuit(outcome.assistantMessage(), "classifier", outcome.decision()));
		}
		return Optional.empty();
	}

}
