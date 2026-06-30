package com.nutriconsultas.mobile.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Deduplicated grocery list for a patient diet assignment (#353).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietGroceryListDto(List<DietGroceryListItemDto> items) {
}
