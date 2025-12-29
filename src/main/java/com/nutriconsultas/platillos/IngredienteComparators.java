package com.nutriconsultas.platillos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IngredienteComparators {

	static Map<ComparatorKey, Comparator<Ingrediente>> map = new HashMap<>();

	static {
		// create a comparator to compare by alimento.nombreAlimento
		final Comparator<Ingrediente> nombreAlimentoComparator = new Comparator<Ingrediente>() {
			@Override
			public int compare(final Ingrediente o1, final Ingrediente o2) {
				return o1.getAlimento().getNombreAlimento().compareTo(o2.getAlimento().getNombreAlimento());
			}
		};

		map.put(new ComparatorKey("ingrediente", Direction.asc), nombreAlimentoComparator);
		map.put(new ComparatorKey("ingrediente", Direction.desc), nombreAlimentoComparator.reversed());

		// compare by cantidad
		map.put(new ComparatorKey("cantidad", Direction.asc), Comparator.comparing(Ingrediente::getCantSugerida));
		map.put(new ComparatorKey("cantidad", Direction.desc),
				Comparator.comparing(Ingrediente::getCantSugerida).reversed());

		// compare by unidad
		map.put(new ComparatorKey("unidad", Direction.asc), Comparator.comparing(Ingrediente::getUnidad));
		map.put(new ComparatorKey("unidad", Direction.desc), Comparator.comparing(Ingrediente::getUnidad).reversed());

		// compare by peso
		map.put(new ComparatorKey("peso", Direction.asc), Comparator.comparing(Ingrediente::getPesoNeto));
		map.put(new ComparatorKey("peso", Direction.desc), Comparator.comparing(Ingrediente::getPesoNeto).reversed());
	}

	public static Comparator<Ingrediente> getComparator(final String name, final Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		return map.get(new ComparatorKey(name, dir));
	}

	private IngredienteComparators() {
	}

}
