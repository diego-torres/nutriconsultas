package com.nutriconsultas.ai;

/**
 * Signals a failure while delivering AI chat SSE events (#435).
 */
public class AiStreamDeliveryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AiStreamDeliveryException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
