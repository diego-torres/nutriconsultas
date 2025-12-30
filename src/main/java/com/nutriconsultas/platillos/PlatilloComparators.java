package com.nutriconsultas.platillos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PlatilloComparators {

	private static final Map<ComparatorKey, Comparator<Platillo>> MAP = new HashMap<>();

	static {
		// compare by name
		MAP.put(new ComparatorKey("platillo", Direction.asc), Comparator.comparing(Platillo::getName));
		MAP.put(new ComparatorKey("platillo", Direction.desc), Comparator.comparing(Platillo::getName).reversed());

		// compare by ingestaSugerida
		MAP.put(new ComparatorKey("ingestas", Direction.asc), Comparator.comparing(Platillo::getIngestasSugeridas));
		MAP.put(new ComparatorKey("ingestas", Direction.desc),
				Comparator.comparing(Platillo::getIngestasSugeridas).reversed());

		// compare by energia
		MAP.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparingInt(Platillo::getEnergia));
		MAP.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparingInt(Platillo::getEnergia).reversed());

		// compare by proteina
		MAP.put(new ComparatorKey("prot", Direction.asc), Comparator.comparingDouble(Platillo::getProteina));
		MAP.put(new ComparatorKey("prot", Direction.desc),
				Comparator.comparingDouble(Platillo::getProteina).reversed());

		// compare by lipidos
		MAP.put(new ComparatorKey("lip", Direction.asc), Comparator.comparingDouble(Platillo::getLipidos));
		MAP.put(new ComparatorKey("lip", Direction.desc), Comparator.comparingDouble(Platillo::getLipidos).reversed());

		// compare by hidratos de carbono
		MAP.put(new ComparatorKey("hc", Direction.asc), Comparator.comparingDouble(Platillo::getHidratosDeCarbono));
		MAP.put(new ComparatorKey("hc", Direction.desc),
				Comparator.comparingDouble(Platillo::getHidratosDeCarbono).reversed());
	}

	public static Comparator<Platillo> getComparator(String name, Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		return MAP.get(new ComparatorKey(name, dir));
	}

	private PlatilloComparators() {
	}

}
