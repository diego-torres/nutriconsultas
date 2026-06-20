package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.dataTables.paging.Direction;

class PlatilloComparatorsTest {

	@Test
	void getComparator_returnsNoOpForUnknownColumn() {
		final Comparator<Platillo> comparator = PlatilloComparators.getComparator("acciones", Direction.asc);

		assertThat(comparator.compare(new Platillo(), new Platillo())).isZero();
	}

}
