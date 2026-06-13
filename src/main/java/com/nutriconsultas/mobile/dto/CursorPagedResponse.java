package com.nutriconsultas.mobile.dto;

import java.util.List;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cursor-based page wrapper for mobile message threads (#96 / #110).
 *
 * @param <T> element type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPagedResponse<T>(List<T> content, String nextCursor, boolean hasMore) {

	public static <T> CursorPagedResponse<T> of(final List<T> content, final String nextCursor) {
		final boolean more = StringUtils.hasText(nextCursor);
		return new CursorPagedResponse<>(content, more ? nextCursor : null, more);
	}

}
