package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;

/**
 * Nutritionist AI chat REST errors (#384).
 */
public class AiChatException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus httpStatus;

	private final AiToolErrorCode errorCode;

	public AiChatException(final HttpStatus httpStatus, final AiToolErrorCode errorCode, final String message) {
		super(message);
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public AiToolErrorCode getErrorCode() {
		return errorCode;
	}

}
