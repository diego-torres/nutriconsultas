package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.dieta.AlimentoIngesta;

/**
 * Patient-facing food item within a meal slot for mobile diet plan detail (#94).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietAlimentoDto(String nombre, Integer porciones, Integer kcal, String unidad) {

	public static DietAlimentoDto fromEntity(final AlimentoIngesta alimento) {
		if (alimento == null) {
			return null;
		}
		return new DietAlimentoDto(alimento.getName(), alimento.getPortions(), alimento.getEnergia(),
				alimento.getUnidad());
	}

}
