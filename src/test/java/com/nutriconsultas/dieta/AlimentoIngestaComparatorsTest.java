package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AlimentoIngestaComparatorsTest {

	@Test
	void byDisplayOrderSortsByOrdenThenId() {
		final AlimentoIngesta first = new AlimentoIngesta();
		first.setId(10L);
		first.setOrden(0);
		first.setName("Manzana");

		final AlimentoIngesta second = new AlimentoIngesta();
		second.setId(5L);
		second.setOrden(1);
		second.setName("Plátano");

		final List<AlimentoIngesta> sorted = List.of(second, first)
			.stream()
			.sorted(AlimentoIngestaComparators.BY_DISPLAY_ORDER)
			.toList();

		assertThat(sorted).containsExactly(first, second);
	}

	@Test
	void nextOrdenReturnsZeroForEmptyCollection() {
		assertThat(AlimentoIngestaComparators.nextOrden(List.of())).isZero();
	}

	@Test
	void nextOrdenReturnsMaxPlusOne() {
		final AlimentoIngesta first = new AlimentoIngesta();
		first.setOrden(0);
		final AlimentoIngesta second = new AlimentoIngesta();
		second.setOrden(2);

		assertThat(AlimentoIngestaComparators.nextOrden(List.of(first, second))).isEqualTo(3);
	}

}
