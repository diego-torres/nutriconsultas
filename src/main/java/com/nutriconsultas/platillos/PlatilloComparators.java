package com.nutriconsultas.platillos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlatilloComparators {

	static Map<ComparatorKey, Comparator<Platillo>> map = new HashMap<>();

	static {
		// compare by name
		map.put(new ComparatorKey("platillo", Direction.asc), Comparator.comparing(Platillo::getName));
		map.put(new ComparatorKey("platillo", Direction.desc), Comparator.comparing(Platillo::getName).reversed());

		// compare by ingestaSugerida
		map.put(new ComparatorKey("ingestas", Direction.asc), Comparator.comparing(Platillo::getIngestasSugeridas));
		map.put(new ComparatorKey("ingestas", Direction.desc),
				Comparator.comparing(Platillo::getIngestasSugeridas).reversed());

		// compare by energia
		map.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparingInt(Platillo::getEnergia));
		map.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparingInt(Platillo::getEnergia).reversed());

		// compare by proteina
		map.put(new ComparatorKey("prot", Direction.asc), Comparator.comparingDouble(Platillo::getProteina));
		map.put(new ComparatorKey("prot", Direction.desc),
				Comparator.comparingDouble(Platillo::getProteina).reversed());

		// compare by lipidos
		map.put(new ComparatorKey("lip", Direction.asc), Comparator.comparingDouble(Platillo::getLipidos));
		map.put(new ComparatorKey("lip", Direction.desc), Comparator.comparingDouble(Platillo::getLipidos).reversed());

		// compare by hidratos de carbono
		map.put(new ComparatorKey("hc", Direction.asc), Comparator.comparingDouble(Platillo::getHidratosDeCarbono));
		map.put(new ComparatorKey("hc", Direction.desc),
				Comparator.comparingDouble(Platillo::getHidratosDeCarbono).reversed());
	}

	public static Comparator<Platillo> getComparator(String name, Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		return map.get(new ComparatorKey(name, dir));
	}

	private PlatilloComparators() {
	}

}
