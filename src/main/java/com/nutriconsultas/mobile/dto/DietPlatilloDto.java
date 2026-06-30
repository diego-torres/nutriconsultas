package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.PlatilloIngesta;

/**
 * Patient-facing dish within a meal slot for mobile diet plan detail (#94, #354).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietPlatilloDto(Long id, String nombre, Integer porciones, Integer kcal, Double proteina,
		Double carbohidratos, Double grasas, String recommendations, String imageUrl) {

	public static DietPlatilloDto fromEntity(final PlatilloIngesta platillo) {
		if (platillo == null) {
			return null;
		}
		return new DietPlatilloDto(platillo.getId(), platillo.getName(), platillo.getPortions(), platillo.getEnergia(),
				platillo.getProteina(), platillo.getHidratosDeCarbono(), platillo.getLipidos(),
				platillo.getRecommendations(), platillo.getImageUrl());
	}

}
