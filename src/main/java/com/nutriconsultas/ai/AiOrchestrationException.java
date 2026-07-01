package com.nutriconsultas.ai;

/**
 * Orchestration errors for the AI chat tool loop (#385).
 */
public class AiOrchestrationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AiOrchestrationException(final String message) {
		super(message);
	}

	public AiOrchestrationException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
