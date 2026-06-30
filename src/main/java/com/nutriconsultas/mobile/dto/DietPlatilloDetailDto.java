package com.nutriconsultas.mobile.dto;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

/**
 * Full platillo detail for mobile diet plan deep links (#352).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloDetailDto(Long id, String nombre, Integer porciones, String imageUrl, String description,
		String videoUrl, String pdfUrl, List<DietPlatilloIngredientDto> ingredientes,
		DietPlatilloNutritionFactsDto nutritionFacts) {

	public static DietPlatilloDetailDto fromEntity(final PlatilloIngesta platillo) {
		if (platillo == null) {
			return null;
		}
		final List<DietPlatilloIngredientDto> ingredientes = platillo.getIngredientes()
			.stream()
			.sorted(Comparator.comparingLong(IngredientePlatilloIngesta::getId))
			.map(DietPlatilloIngredientDto::fromEntity)
			.toList();
		return new DietPlatilloDetailDto(platillo.getId(), platillo.getName(), platillo.getPortions(),
				platillo.getImageUrl(), platillo.getRecommendations(), platillo.getVideoUrl(), platillo.getPdfUrl(),
				ingredientes, DietPlatilloNutritionFactsDto.fromEntity(platillo));
	}

}
