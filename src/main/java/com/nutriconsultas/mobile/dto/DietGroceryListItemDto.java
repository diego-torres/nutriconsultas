package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Aggregated grocery row for a patient diet plan (#353).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietGroceryListItemDto(String nombre, String cantidad, String unidad, String categoria) {
}
