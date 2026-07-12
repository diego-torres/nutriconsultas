package com.nutriconsultas.platillos;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IngredienteComparators {

	public static final Comparator<Ingrediente> BY_DISPLAY_ORDER = Comparator
		.comparing(Ingrediente::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(Ingrediente::getId, Comparator.nullsLast(Comparator.naturalOrder()));

	private static final Map<ComparatorKey, Comparator<Ingrediente>> MAP = new HashMap<>();

	static {
		MAP.put(new ComparatorKey("orden", Direction.asc), BY_DISPLAY_ORDER);
		MAP.put(new ComparatorKey("orden", Direction.desc), BY_DISPLAY_ORDER.reversed());

		// create a comparator to compare by alimento.nombreAlimento
		final Comparator<Ingrediente> nombreAlimentoComparator = new Comparator<Ingrediente>() {
			@Override
			public int compare(final Ingrediente o1, final Ingrediente o2) {
				return o1.getAlimento().getNombreAlimento().compareTo(o2.getAlimento().getNombreAlimento());
			}
		};

		MAP.put(new ComparatorKey("ingrediente", Direction.asc), nombreAlimentoComparator);
		MAP.put(new ComparatorKey("ingrediente", Direction.desc), nombreAlimentoComparator.reversed());

		// compare by cantidad
		MAP.put(new ComparatorKey("cantidad", Direction.asc), Comparator.comparing(Ingrediente::getCantSugerida));
		MAP.put(new ComparatorKey("cantidad", Direction.desc),
				Comparator.comparing(Ingrediente::getCantSugerida).reversed());

		// compare by unidad
		MAP.put(new ComparatorKey("unidad", Direction.asc), Comparator.comparing(Ingrediente::getUnidad));
		MAP.put(new ComparatorKey("unidad", Direction.desc), Comparator.comparing(Ingrediente::getUnidad).reversed());

		// compare by peso
		MAP.put(new ComparatorKey("peso", Direction.asc), Comparator.comparing(Ingrediente::getPesoNeto));
		MAP.put(new ComparatorKey("peso", Direction.desc), Comparator.comparing(Ingrediente::getPesoNeto).reversed());
	}

	public static Comparator<Ingrediente> getComparator(final String name, final Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		final Comparator<Ingrediente> comparator = MAP.get(new ComparatorKey(name, dir));
		return comparator != null ? comparator : BY_DISPLAY_ORDER;
	}

	public static int nextOrden(final Collection<Ingrediente> existing) {
		if (existing == null || existing.isEmpty()) {
			return 0;
		}
		return existing.stream().map(Ingrediente::getOrden).filter(Objects::nonNull).max(Integer::compareTo).orElse(-1)
				+ 1;
	}

	private IngredienteComparators() {
	}

}
