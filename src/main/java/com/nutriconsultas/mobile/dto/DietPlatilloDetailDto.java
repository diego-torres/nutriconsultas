package com.nutriconsultas.mobile.dto;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaPictureSupport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Full platillo detail for mobile diet plan deep links (#352, #598).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloDetailDto(Long id, String nombre, Integer porciones,
		@Schema(description = "Fetchable mobile or static placeholder image path",
				example = "/rest/mobile/patient/diet-plans/7/platillos/30/image") String imageUrl,
		String description, String videoUrl, String pdfUrl, List<DietPlatilloIngredientDto> ingredientes,
		DietPlatilloNutritionFactsDto nutritionFacts) {

	public static DietPlatilloDetailDto fromEntity(final PlatilloIngesta platillo, final Long assignmentId) {
		if (platillo == null) {
			return null;
		}
		final List<DietPlatilloIngredientDto> ingredientes = platillo.getIngredientes()
			.stream()
			.sorted(Comparator.comparingLong(IngredientePlatilloIngesta::getId))
			.map(DietPlatilloIngredientDto::fromEntity)
			.toList();
		return new DietPlatilloDetailDto(platillo.getId(), platillo.getName(), platillo.getPortions(),
				PlatilloIngestaPictureSupport.resolveDisplayUrlForMobile(assignmentId, platillo),
				platillo.getRecommendations(), platillo.getVideoUrl(), platillo.getPdfUrl(), ingredientes,
				DietPlatilloNutritionFactsDto.fromEntity(platillo));
	}

}
