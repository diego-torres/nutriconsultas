package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IngestaComparatorsTest {

	@Test
	void byDisplayOrderSortsByOrdenThenId() {
		final Ingesta first = new Ingesta();
		first.setId(10L);
		first.setOrden(0);
		first.setNombre("Desayuno");

		final Ingesta second = new Ingesta();
		second.setId(5L);
		second.setOrden(1);
		second.setNombre("Comida");

		final List<Ingesta> sorted = List.of(second, first)
			.stream()
			.sorted(IngestaComparators.BY_DISPLAY_ORDER)
			.toList();

		assertThat(sorted).containsExactly(first, second);
	}

}
