package com.nutriconsultas.mobile;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;

/**
 * Aggregates platillo ingredients and standalone ingesta alimentos into a deduplicated
 * grocery list (#353).
 */
public final class DietGroceryListAggregator {

	private DietGroceryListAggregator() {
	}

	public static List<DietGroceryListItemDto> aggregate(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return List.of();
		}
		final Map<String, MutableGroceryItem> items = new LinkedHashMap<>();
		for (final Ingesta ingesta : dieta.getIngestas()) {
			aggregateIngesta(ingesta, items);
		}
		return items.values()
			.stream()
			.sorted(Comparator.comparing(MutableGroceryItem::nombre, String.CASE_INSENSITIVE_ORDER))
			.map(MutableGroceryItem::toDto)
			.toList();
	}

	private static void aggregateIngesta(final Ingesta ingesta, final Map<String, MutableGroceryItem> items) {
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				aggregatePlatillo(platillo, items);
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				aggregateStandaloneAlimento(alimento, items);
			}
		}
	}

	private static void aggregatePlatillo(final PlatilloIngesta platillo, final Map<String, MutableGroceryItem> items) {
		if (platillo.getIngredientes() == null) {
			return;
		}
		final int portions = platillo.getPortions() != null ? platillo.getPortions() : 1;
		for (final IngredientePlatilloIngesta ingrediente : platillo.getIngredientes()) {
			addPlatilloIngredient(items, ingrediente, portions);
		}
	}

	private static void addPlatilloIngredient(final Map<String, MutableGroceryItem> items,
			final IngredientePlatilloIngesta ingrediente, final int portions) {
		final Alimento alimento = ingrediente.getAlimento();
		final String nombre = alimento != null ? alimento.getNombreAlimento() : ingrediente.getDescription();
		final String unidad = ingrediente.getUnidad();
		if (nombre == null || nombre.isBlank()) {
			return;
		}
		final Long alimentoId = alimento != null ? alimento.getId() : null;
		final String categoria = alimento != null ? alimento.getClasificacion() : null;
		final String key = buildKey(alimentoId, nombre, unidad);
		final MutableGroceryItem item = items.computeIfAbsent(key,
				ignored -> new MutableGroceryItem(nombre, unidad, categoria));
		item.mergeCategoria(categoria);
		if (ingrediente.shouldDisplayWeightInGrams(unidad)) {
			final Integer peso = ingrediente.getPesoBrutoRedondeado();
			if (peso != null) {
				item.addGramQuantity(peso * portions);
			}
		}
		else if (ingrediente.getCantSugerida() != null) {
			item.addFractionQuantity(ingrediente.getCantSugerida() * portions);
		}
	}

	private static void aggregateStandaloneAlimento(final AlimentoIngesta alimentoIngesta,
			final Map<String, MutableGroceryItem> items) {
		final Alimento alimento = alimentoIngesta.getAlimento();
		final String nombre = alimento != null ? alimento.getNombreAlimento() : alimentoIngesta.getName();
		final String unidad = alimentoIngesta.getUnidad();
		if (nombre == null || nombre.isBlank()) {
			return;
		}
		final Long alimentoId = alimento != null ? alimento.getId() : null;
		final String categoria = alimento != null ? alimento.getClasificacion() : null;
		final String key = buildKey(alimentoId, nombre, unidad);
		final MutableGroceryItem item = items.computeIfAbsent(key,
				ignored -> new MutableGroceryItem(nombre, unidad, categoria));
		item.mergeCategoria(categoria);
		final int portions = alimentoIngesta.getPortions() != null ? alimentoIngesta.getPortions() : 1;
		item.addFractionQuantity(portions);
	}

	private static String buildKey(final Long alimentoId, final String nombre, final String unidad) {
		final String unit = unidad != null ? unidad : "";
		if (alimentoId != null) {
			return "a:" + alimentoId + "|" + unit;
		}
		return "n:" + nombre + "|" + unit;
	}

	private static final class MutableGroceryItem {

		private final String nombre;

		private final String unidad;

		private String categoria;

		private double fractionQuantity;

		private int gramQuantity;

		private boolean usesGramQuantity;

		private MutableGroceryItem(final String nombre, final String unidad, final String categoria) {
			this.nombre = nombre;
			this.unidad = unidad;
			this.categoria = categoria;
		}

		private void mergeCategoria(final String nextCategoria) {
			if (categoria == null && nextCategoria != null) {
				categoria = nextCategoria;
			}
		}

		private void addFractionQuantity(final double amount) {
			if (usesGramQuantity) {
				return;
			}
			fractionQuantity += amount;
		}

		private void addGramQuantity(final int grams) {
			usesGramQuantity = true;
			gramQuantity += grams;
		}

		private String nombre() {
			return nombre;
		}

		private DietGroceryListItemDto toDto() {
			final String cantidad;
			if (usesGramQuantity) {
				cantidad = String.valueOf(gramQuantity);
			}
			else {
				final IngredientePlatilloIngesta helper = new IngredientePlatilloIngesta();
				helper.setCantSugerida(fractionQuantity);
				cantidad = helper.getDisplayCantSugerida(unidad);
			}
			return new DietGroceryListItemDto(nombre, cantidad, unidad, categoria);
		}

	}

}
