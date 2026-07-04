package com.nutriconsultas.ai;

/**
 * Deterministic scope violation detected before OpenAI orchestration (#447).
 */
public record AiRequestScopeViolation(AiRequestScopeKind kind, int requestedAmount, String refusalMessage) {
}
