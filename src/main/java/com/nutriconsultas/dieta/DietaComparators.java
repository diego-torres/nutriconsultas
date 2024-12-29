package com.nutriconsultas.dieta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class DietaComparators {
    static Map<ComparatorKey, Comparator<Dieta>> map = new HashMap<>();

    static {
        map.put(new ComparatorKey("nombre", Direction.asc), Comparator.comparing(Dieta::getNombre));
        map.put(new ComparatorKey("nombre", Direction.desc), Comparator.comparing(Dieta::getNombre).reversed());
    }

    public static Comparator<Dieta> getComparator(String name, Direction dir) {
        return map.get(new ComparatorKey(name, dir));
    }

    private DietaComparators() {
    }
}
