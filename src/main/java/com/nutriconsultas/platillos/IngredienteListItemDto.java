package com.nutriconsultas.platillos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.alimentos.Alimento;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngredienteListItemDto(Long id, String nombre, String cantidad, String unidad, Integer peso,
		Double alimentoCantSugerida, Integer alimentoPesoNeto) {

	public static IngredienteListItemDto fromEntity(final Ingrediente ingrediente) {
		if (ingrediente == null) {
			return null;
		}
		final Alimento alimento = ingrediente.getAlimento();
		final String nombre = alimento != null ? alimento.getNombreAlimento() : ingrediente.getDescription();
		final Double alimentoCant = alimento != null ? alimento.getCantSugerida() : null;
		final Integer alimentoPeso = alimento != null ? alimento.getPesoNeto() : null;
		return new IngredienteListItemDto(ingrediente.getId(), nombre, ingrediente.getFractionalCantSugerida(),
				ingrediente.getUnidad(), ingrediente.getPesoNeto(), alimentoCant, alimentoPeso);
	}

}
