package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;

/**
 * Ingredient row within a patient diet platillo detail (#352).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloIngredientDto(String nombre, String cantidad, String unidad) {

	public static DietPlatilloIngredientDto fromEntity(final IngredientePlatilloIngesta ingrediente) {
		if (ingrediente == null) {
			return null;
		}
		final String nombre = ingrediente.getAlimento() != null ? ingrediente.getAlimento().getNombreAlimento()
				: ingrediente.getDescription();
		return new DietPlatilloIngredientDto(nombre, ingrediente.getDisplayCantSugerida(ingrediente.getUnidad()),
				ingrediente.getUnidad());
	}

}
