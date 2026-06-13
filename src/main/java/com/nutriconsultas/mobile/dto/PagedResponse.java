package com.nutriconsultas.mobile.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Offset-based page wrapper for mobile list endpoints (#110).
 *
 * @param <T> element type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

	@SuppressWarnings("PMD.ShortMethodName")
	public static <T> PagedResponse<T> of(final Page<T> page) {
		return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages(), page.isLast());
	}

}
