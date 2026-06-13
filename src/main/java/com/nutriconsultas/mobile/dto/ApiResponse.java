package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard envelope for {@code /rest/mobile/**} JSON responses (#110).
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String message, Instant timestamp) {

	@SuppressWarnings("PMD.ShortMethodName")
	public static <T> ApiResponse<T> ok(final T data) {
		return new ApiResponse<>(data, null, Instant.now());
	}

	public static <T> ApiResponse<T> withMessage(final T data, final String message) {
		return new ApiResponse<>(data, message, Instant.now());
	}

}
