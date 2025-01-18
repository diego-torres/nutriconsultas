package com.nutriconsultas.dieta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class DietaComparators {
    static Map<ComparatorKey, Comparator<Dieta>> map = new HashMap<>();

    static {
        map.put(new ComparatorKey("dieta", Direction.asc), Comparator.comparing(Dieta::getNombre));
        map.put(new ComparatorKey("dieta", Direction.desc), Comparator.comparing(Dieta::getNombre).reversed());

        // compare by nombre ingestas
        Comparator<Dieta> byIngestas = (d1, d2) -> {
            String i1 = d1.getIngestas().stream().map(Ingesta::getNombre).reduce("", String::concat);
            String i2 = d2.getIngestas().stream().map(Ingesta::getNombre).reduce("", String::concat);
            return i1.compareTo(i2);
        };
        map.put(new ComparatorKey("ingestas", Direction.asc), byIngestas);
        map.put(new ComparatorKey("ingestas", Direction.desc), byIngestas.reversed());

        // compare kcal
        map.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparing(Dieta::getEnergia));
        map.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparing(Dieta::getEnergia).reversed());

        // compare proteina
        map.put(new ComparatorKey("prot", Direction.asc), Comparator.comparing(Dieta::getProteina));
        map.put(new ComparatorKey("prot", Direction.desc), Comparator.comparing(Dieta::getProteina).reversed());

        // compare lipidos
        map.put(new ComparatorKey("lip", Direction.asc), Comparator.comparing(Dieta::getLipidos));
        map.put(new ComparatorKey("lip", Direction.desc), Comparator.comparing(Dieta::getLipidos).reversed());

        // compare hidratos de carbono
        map.put(new ComparatorKey("hc", Direction.asc), Comparator.comparing(Dieta::getHidratosDeCarbono));
        map.put(new ComparatorKey("hc", Direction.desc), Comparator.comparing(Dieta::getHidratosDeCarbono).reversed());
    }

    public static Comparator<Dieta> getComparator(String name, Direction dir) {
        return map.get(new ComparatorKey(name, dir));
    }

    private DietaComparators() {
    }
}
