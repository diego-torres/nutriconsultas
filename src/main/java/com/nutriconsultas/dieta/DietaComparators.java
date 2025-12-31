package com.nutriconsultas.dieta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class DietaComparators {

	private static final Map<ComparatorKey, Comparator<Dieta>> MAP = new HashMap<>();

	static {
		MAP.put(new ComparatorKey("dieta", Direction.asc), Comparator.comparing(Dieta::getNombre));
		MAP.put(new ComparatorKey("dieta", Direction.desc), Comparator.comparing(Dieta::getNombre).reversed());

		// compare by nombre ingestas
		final Comparator<Dieta> byIngestas = (final Dieta d1, final Dieta d2) -> {
			final String i1 = d1.getIngestas().stream().map(Ingesta::getNombre).reduce("", String::concat);
			final String i2 = d2.getIngestas().stream().map(Ingesta::getNombre).reduce("", String::concat);
			return i1.compareTo(i2);
		};
		MAP.put(new ComparatorKey("ingestas", Direction.asc), byIngestas);
		MAP.put(new ComparatorKey("ingestas", Direction.desc), byIngestas.reversed());

		// compare kcal
		MAP.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparing(Dieta::getEnergia));
		MAP.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparing(Dieta::getEnergia).reversed());

		// compare proteina
		MAP.put(new ComparatorKey("prot", Direction.asc), Comparator.comparing(Dieta::getProteina));
		MAP.put(new ComparatorKey("prot", Direction.desc), Comparator.comparing(Dieta::getProteina).reversed());

		// compare lipidos
		MAP.put(new ComparatorKey("lip", Direction.asc), Comparator.comparing(Dieta::getLipidos));
		MAP.put(new ComparatorKey("lip", Direction.desc), Comparator.comparing(Dieta::getLipidos).reversed());

		// compare hidratos de carbono
		MAP.put(new ComparatorKey("hc", Direction.asc), Comparator.comparing(Dieta::getHidratosDeCarbono));
		MAP.put(new ComparatorKey("hc", Direction.desc), Comparator.comparing(Dieta::getHidratosDeCarbono).reversed());
	}

	public static Comparator<Dieta> getComparator(final String name, final Direction dir) {
		final Comparator<Dieta> comparator = MAP.get(new ComparatorKey(name, dir));
		return comparator != null ? comparator : (o1, o2) -> 0;
	}

	private DietaComparators() {
	}

}
