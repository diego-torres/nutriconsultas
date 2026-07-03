package com.nutriconsultas.ai;

/**
 * Signals a failure while delivering AI chat SSE events (#435).
 */
public class AiStreamDeliveryException extends RuntimeException {

	public AiStreamDeliveryException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
