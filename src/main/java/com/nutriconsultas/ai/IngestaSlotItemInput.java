package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Item inside an {@link IngestaSlotInput} for plan validation.
 */
public record IngestaSlotItemInput(String type, @Nullable Long platilloId, @Nullable Long alimentoId,
		@Nullable Integer portions, @Nullable List<RecipeIngredientInput> ingredients) {
}
