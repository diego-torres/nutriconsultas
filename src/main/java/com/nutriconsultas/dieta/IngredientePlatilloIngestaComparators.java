package com.nutriconsultas.dieta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IngredientePlatilloIngestaComparators {

	private static final Map<ComparatorKey, Comparator<IngredientePlatilloIngesta>> MAP = new HashMap<>();

	static {
		final Comparator<IngredientePlatilloIngesta> nombreAlimentoComparator = (o1,
				o2) -> o1.getAlimento().getNombreAlimento().compareTo(o2.getAlimento().getNombreAlimento());

		MAP.put(new ComparatorKey("ingrediente", Direction.asc), nombreAlimentoComparator);
		MAP.put(new ComparatorKey("ingrediente", Direction.desc), nombreAlimentoComparator.reversed());
		MAP.put(new ComparatorKey("cantidad", Direction.asc),
				Comparator.comparing(IngredientePlatilloIngesta::getCantSugerida));
		MAP.put(new ComparatorKey("cantidad", Direction.desc),
				Comparator.comparing(IngredientePlatilloIngesta::getCantSugerida).reversed());
		MAP.put(new ComparatorKey("unidad", Direction.asc),
				Comparator.comparing(IngredientePlatilloIngesta::getUnidad));
		MAP.put(new ComparatorKey("unidad", Direction.desc),
				Comparator.comparing(IngredientePlatilloIngesta::getUnidad).reversed());
		MAP.put(new ComparatorKey("peso", Direction.asc),
				Comparator.comparing(IngredientePlatilloIngesta::getPesoNeto));
		MAP.put(new ComparatorKey("peso", Direction.desc),
				Comparator.comparing(IngredientePlatilloIngesta::getPesoNeto).reversed());
	}

	public static Comparator<IngredientePlatilloIngesta> getComparator(final String name, final Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		return MAP.get(new ComparatorKey(name, dir));
	}

	private IngredientePlatilloIngestaComparators() {
	}

}
