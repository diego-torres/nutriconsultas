package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaPictureSupport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Patient-facing dish within a meal slot for mobile diet plan detail (#94, #354, #598).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloDto(Long id, String nombre, Integer porciones, Integer kcal, Double proteina,
		Double carbohidratos, Double grasas, String recommendations,
		@Schema(description = "Fetchable mobile or static placeholder image path",
				example = "/rest/mobile/patient/diet-plans/7/platillos/30/image") String imageUrl) {

	public static DietPlatilloDto fromEntity(final PlatilloIngesta platillo, final Long assignmentId) {
		if (platillo == null) {
			return null;
		}
		return new DietPlatilloDto(platillo.getId(), platillo.getName(), platillo.getPortions(), platillo.getEnergia(),
				platillo.getProteina(), platillo.getHidratosDeCarbono(), platillo.getLipidos(),
				platillo.getRecommendations(),
				PlatilloIngestaPictureSupport.resolveDisplayUrlForMobile(assignmentId, platillo));
	}

}
