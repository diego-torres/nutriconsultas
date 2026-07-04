package com.nutriconsultas.ai;

/**
 * Raised when an AI chat stream is cancelled before the assistant reply is persisted
 * (#436).
 */
public class AiStreamCancelledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AiStreamCancelledException() {
		super("Generación cancelada.");
	}

}
