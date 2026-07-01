package com.nutriconsultas.ai;

/**
 * Draft lifecycle errors for nutritionist-scoped AI drafts (#371).
 */
public class AiDraftLifecycleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AiDraftLifecycleException(final String message) {
		super(message);
	}

}
