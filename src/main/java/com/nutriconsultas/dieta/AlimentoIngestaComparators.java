package com.nutriconsultas.dieta;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

public final class AlimentoIngestaComparators {

	public static final Comparator<AlimentoIngesta> BY_DISPLAY_ORDER = Comparator
		.comparing(AlimentoIngesta::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(AlimentoIngesta::getId, Comparator.nullsLast(Comparator.naturalOrder()));

	public static int nextOrden(final Collection<AlimentoIngesta> existing) {
		if (existing == null || existing.isEmpty()) {
			return 0;
		}
		return existing.stream()
			.map(AlimentoIngesta::getOrden)
			.filter(Objects::nonNull)
			.max(Integer::compareTo)
			.orElse(-1) + 1;
	}

	private AlimentoIngestaComparators() {
	}

}
