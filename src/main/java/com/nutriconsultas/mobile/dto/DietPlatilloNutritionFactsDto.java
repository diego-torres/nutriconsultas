package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.PlatilloIngesta;

/**
 * Per-platillo nutrition summary for mobile detail (#352).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloNutritionFactsDto(Integer kcal, Double proteina, Double carbohidratos, Double grasas,
		Double fibra, Double sodio) {

	public static DietPlatilloNutritionFactsDto fromEntity(final PlatilloIngesta platillo) {
		if (platillo == null) {
			return null;
		}
		return new DietPlatilloNutritionFactsDto(platillo.getEnergia(), platillo.getProteina(),
				platillo.getHidratosDeCarbono(), platillo.getLipidos(), platillo.getFibra(), platillo.getSodio());
	}

}
