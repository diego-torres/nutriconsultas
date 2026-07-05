package com.nutriconsultas.ai.mcp;

import org.springframework.http.HttpStatus;

/**
 * Access failure for MCP requests (#394).
 */
public final class McpAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;

	private final String userMessage;

	public McpAccessException(final HttpStatus status, final String userMessage) {
		super(userMessage);
		this.status = status;
		this.userMessage = userMessage;
	}

	public McpAccessException(final HttpStatus status, final String userMessage, final Throwable cause) {
		super(userMessage, cause);
		this.status = status;
		this.userMessage = userMessage;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getUserMessage() {
		return userMessage;
	}

}
