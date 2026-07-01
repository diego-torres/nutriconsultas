package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;

/**
 * OpenAI client failure mapped for orchestration and user-facing Spanish messages (#366).
 */
public class OpenAiClientException extends RuntimeException {

	public enum ErrorKind {

		NOT_CONFIGURED, AUTH, RATE_LIMIT, MODEL_NOT_FOUND, TIMEOUT, UNAVAILABLE, INVALID_REQUEST, UNKNOWN

	}

	private final ErrorKind kind;

	private final HttpStatus httpStatus;

	private final String userMessage;

	public OpenAiClientException(final ErrorKind kind, final HttpStatus httpStatus, final String userMessage,
			final String logMessage, final Throwable cause) {
		super(logMessage, cause);
		this.kind = kind;
		this.httpStatus = httpStatus;
		this.userMessage = userMessage;
	}

	public ErrorKind getKind() {
		return kind;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getUserMessage() {
		return userMessage;
	}

}
