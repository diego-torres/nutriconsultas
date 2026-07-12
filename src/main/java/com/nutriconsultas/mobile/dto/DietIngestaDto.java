package com.nutriconsultas.mobile.dto;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.AlimentoIngestaComparators;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

/**
 * Meal slot within a diet plan for mobile diet plan detail (#94).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietIngestaDto(String tipo, Integer totalKcal, Double totalProteina, Double totalGrasas,
		Double totalCarbohidratos, List<DietPlatilloDto> platillos, List<DietAlimentoDto> alimentos) {

	public static DietIngestaDto fromEntity(final Ingesta ingesta) {
		if (ingesta == null) {
			return null;
		}
		final List<DietPlatilloDto> platillos = ingesta.getPlatillos()
			.stream()
			.sorted(Comparator.comparingLong(PlatilloIngesta::getId))
			.map(DietPlatilloDto::fromEntity)
			.toList();
		final List<DietAlimentoDto> alimentos = ingesta.getAlimentos()
			.stream()
			.sorted(AlimentoIngestaComparators.BY_DISPLAY_ORDER)
			.map(DietAlimentoDto::fromEntity)
			.toList();
		return new DietIngestaDto(ingesta.getNombre(), ingesta.getEnergia(), ingesta.getProteina(),
				ingesta.getLipidos(), ingesta.getHidratosDeCarbono(), platillos, alimentos);
	}

}
