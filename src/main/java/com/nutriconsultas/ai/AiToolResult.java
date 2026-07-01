package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Common success/error envelope for AI tool handlers.
 */
public record AiToolResult<T>(boolean success, @Nullable T data, @Nullable AiToolErrorCode errorCode,
		@Nullable String message) {

	public static <T> AiToolResult<T> success(final T data) {
		return new AiToolResult<>(true, data, null, null);
	}

	public static <T> AiToolResult<T> error(final AiToolErrorCode errorCode, final String message) {
		return new AiToolResult<>(false, null, errorCode, message);
	}

}
