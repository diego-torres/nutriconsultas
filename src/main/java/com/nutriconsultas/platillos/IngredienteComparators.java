package com.nutriconsultas.platillos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IngredienteComparators {

	private static final Map<ComparatorKey, Comparator<Ingrediente>> MAP = new HashMap<>();

	static {
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
		return MAP.get(new ComparatorKey(name, dir));
	}

	private IngredienteComparators() {
	}

}
