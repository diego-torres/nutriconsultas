package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;

class DietGroceryListAggregatorTest {

	@Test
	void aggregate_deduplicatesByAlimentoAndUnitAndScalesPlatilloPortions() {
		final Alimento avena = alimento(1L, "Avena", "Cereales");
		final Dieta dieta = new Dieta();
		final Ingesta desayuno = ingesta("Desayuno");
		final Ingesta colacion = ingesta("Colación");

		desayuno.setPlatillos(List.of(platilloWithIngredient(avena, 0.5, "taza", 2)));
		colacion.setPlatillos(List.of(platilloWithIngredient(avena, 1.0, "taza", 1)));
		dieta.setIngestas(List.of(desayuno, colacion));

		final List<DietGroceryListItemDto> items = DietGroceryListAggregator.aggregate(dieta);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).nombre()).isEqualTo("Avena");
		assertThat(items.get(0).unidad()).isEqualTo("taza");
		assertThat(items.get(0).categoria()).isEqualTo("Cereales");
		assertThat(items.get(0).cantidad()).isEqualTo("2");
	}

	@Test
	void aggregate_includesStandaloneAlimentos() {
		final Alimento manzana = alimento(2L, "Manzana", "Frutas");
		final Dieta dieta = new Dieta();
		final Ingesta ingesta = ingesta("Desayuno");
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setName("Manzana");
		alimentoIngesta.setPortions(2);
		alimentoIngesta.setUnidad("pieza");
		alimentoIngesta.setAlimento(manzana);
		ingesta.setAlimentos(List.of(alimentoIngesta));
		dieta.setIngestas(List.of(ingesta));

		final List<DietGroceryListItemDto> items = DietGroceryListAggregator.aggregate(dieta);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).nombre()).isEqualTo("Manzana");
		assertThat(items.get(0).cantidad()).isEqualTo("2");
		assertThat(items.get(0).unidad()).isEqualTo("pieza");
		assertThat(items.get(0).categoria()).isEqualTo("Frutas");
	}

	@Test
	void aggregate_mergesMultipleDiets() {
		final Alimento avena = alimento(1L, "Avena", "Cereales");
		final Alimento manzana = alimento(2L, "Manzana", "Frutas");

		final Dieta lunes = new Dieta();
		final Ingesta lunesIngesta = ingesta("Desayuno");
		lunesIngesta.setAlimentos(List.of(standaloneAlimento(avena, 1)));
		lunes.setIngestas(List.of(lunesIngesta));

		final Dieta martes = new Dieta();
		final Ingesta martesIngesta = ingesta("Colación");
		martesIngesta.setAlimentos(List.of(standaloneAlimento(manzana, 2)));
		martes.setIngestas(List.of(martesIngesta));

		final List<DietGroceryListItemDto> items = DietGroceryListAggregator.aggregate(List.of(lunes, martes));

		assertThat(items).hasSize(2);
		assertThat(items).extracting(DietGroceryListItemDto::nombre).containsExactlyInAnyOrder("Avena", "Manzana");
	}

	@Test
	void aggregate_returnsEmptyListWhenPlanHasNoIngredients() {
		final Dieta dieta = new Dieta();
		final Ingesta ingesta = ingesta("Desayuno");
		ingesta.setPlatillos(List.of(new PlatilloIngesta()));
		dieta.setIngestas(List.of(ingesta));

		assertThat(DietGroceryListAggregator.aggregate(dieta)).isEmpty();
	}

	private static AlimentoIngesta standaloneAlimento(final Alimento alimento, final int portions) {
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setName(alimento.getNombreAlimento());
		alimentoIngesta.setPortions(portions);
		alimentoIngesta.setUnidad("pieza");
		alimentoIngesta.setAlimento(alimento);
		return alimentoIngesta;
	}

	private static Alimento alimento(final Long id, final String nombre, final String categoria) {
		final Alimento alimento = new Alimento();
		alimento.setId(id);
		alimento.setNombreAlimento(nombre);
		alimento.setClasificacion(categoria);
		return alimento;
	}

	private static Ingesta ingesta(final String nombre) {
		final Ingesta ingesta = new Ingesta(nombre);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());
		return ingesta;
	}

	private static PlatilloIngesta platilloWithIngredient(final Alimento alimento, final double cantidad,
			final String unidad, final int portions) {
		final IngredientePlatilloIngesta ingrediente = new IngredientePlatilloIngesta();
		ingrediente.setAlimento(alimento);
		ingrediente.setCantSugerida(cantidad);
		ingrediente.setUnidad(unidad);
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setPortions(portions);
		platillo.setIngredientes(new ArrayList<>(List.of(ingrediente)));
		ingrediente.setPlatillo(platillo);
		return platillo;
	}

}
