package com.nutriconsultas.dieta;

import java.util.Comparator;

public final class IngestaComparators {

	public static final Comparator<Ingesta> BY_DISPLAY_ORDER = Comparator
		.comparing(Ingesta::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(Ingesta::getId, Comparator.nullsLast(Comparator.naturalOrder()));

	private IngestaComparators() {
	}

}
