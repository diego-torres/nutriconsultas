package com.nutriconsultas.alimentos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AlimentoComparators {

	private static final Map<ComparatorKey, Comparator<Alimento>> MAP = new HashMap<>();

	static {
		MAP.put(new ComparatorKey("alimento", Direction.asc), Comparator.comparing(Alimento::getNombreAlimento));
		MAP.put(new ComparatorKey("alimento", Direction.desc),
				Comparator.comparing(Alimento::getNombreAlimento).reversed());

		MAP.put(new ComparatorKey("grupo", Direction.asc), Comparator.comparing(Alimento::getNombreAlimento));
		MAP.put(new ComparatorKey("grupo", Direction.desc),
				Comparator.comparing(Alimento::getNombreAlimento).reversed());

		MAP.put(new ComparatorKey("cantidad", Direction.asc), Comparator.comparing(Alimento::getCantSugerida));
		MAP.put(new ComparatorKey("cantidad", Direction.desc),
				Comparator.comparing(Alimento::getCantSugerida).reversed());

		MAP.put(new ComparatorKey("unidad", Direction.asc), Comparator.comparing(Alimento::getUnidad));
		MAP.put(new ComparatorKey("unidad", Direction.desc), Comparator.comparing(Alimento::getUnidad).reversed());

		MAP.put(new ComparatorKey("bruto", Direction.asc), Comparator.comparingInt(Alimento::getPesoBrutoRedondeado));
		MAP.put(new ComparatorKey("bruto", Direction.desc),
				Comparator.comparingInt(Alimento::getPesoBrutoRedondeado).reversed());

		MAP.put(new ComparatorKey("neto", Direction.asc), Comparator.comparingInt(Alimento::getPesoNeto));
		MAP.put(new ComparatorKey("neto", Direction.desc), Comparator.comparingInt(Alimento::getPesoNeto).reversed());

		MAP.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparingInt(Alimento::getEnergia));
		MAP.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparingInt(Alimento::getEnergia).reversed());

		MAP.put(new ComparatorKey("prot", Direction.asc), Comparator.comparingDouble(Alimento::getProteina));
		MAP.put(new ComparatorKey("prot", Direction.desc),
				Comparator.comparingDouble(Alimento::getProteina).reversed());

		MAP.put(new ComparatorKey("lip", Direction.asc), Comparator.comparingDouble(Alimento::getLipidos));
		MAP.put(new ComparatorKey("lip", Direction.desc), Comparator.comparingDouble(Alimento::getLipidos).reversed());

		MAP.put(new ComparatorKey("hc", Direction.asc), Comparator.comparingDouble(Alimento::getHidratosDeCarbono));
		MAP.put(new ComparatorKey("hc", Direction.desc),
				Comparator.comparingDouble(Alimento::getHidratosDeCarbono).reversed());
	}

	public static Comparator<Alimento> getComparator(String name, Direction dir) {
		log.debug("comparator request name: {}, dir: {}", name, dir);
		return MAP.get(new ComparatorKey(name, dir));
	}

	private AlimentoComparators() {
	}

}
