package com.nutriconsultas.alimentos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public class AlimentoComparators {
  static Map<ComparatorKey, Comparator<Alimento>> map = new HashMap<>();

  static {
    map.put(new ComparatorKey("alimento", Direction.asc), Comparator.comparing(Alimento::getNombreAlimento));
    map.put(new ComparatorKey("alimento", Direction.desc),
        Comparator.comparing(Alimento::getNombreAlimento).reversed());

    map.put(new ComparatorKey("grupo", Direction.asc), Comparator.comparing(Alimento::getNombreAlimento));
    map.put(new ComparatorKey("grupo", Direction.desc), Comparator.comparing(Alimento::getNombreAlimento).reversed());

    map.put(new ComparatorKey("cantidad", Direction.asc), Comparator.comparing(Alimento::getCantSugerida));
    map.put(new ComparatorKey("cantidad", Direction.desc), Comparator.comparing(Alimento::getCantSugerida).reversed());

    map.put(new ComparatorKey("unidad", Direction.asc), Comparator.comparing(Alimento::getUnidad));
    map.put(new ComparatorKey("unidad", Direction.desc), Comparator.comparing(Alimento::getUnidad).reversed());

    map.put(new ComparatorKey("bruto", Direction.asc), Comparator.comparingInt(Alimento::getPesoBrutoRedondeado));
    map.put(new ComparatorKey("bruto", Direction.desc),
        Comparator.comparingInt(Alimento::getPesoBrutoRedondeado).reversed());

    map.put(new ComparatorKey("neto", Direction.asc), Comparator.comparingInt(Alimento::getPesoNeto));
    map.put(new ComparatorKey("neto", Direction.desc), Comparator.comparingInt(Alimento::getPesoNeto).reversed());

    map.put(new ComparatorKey("kcal", Direction.asc), Comparator.comparingInt(Alimento::getEnergia));
    map.put(new ComparatorKey("kcal", Direction.desc), Comparator.comparingInt(Alimento::getEnergia).reversed());

    map.put(new ComparatorKey("prot", Direction.asc), Comparator.comparingDouble(Alimento::getProteina));
    map.put(new ComparatorKey("prot", Direction.desc), Comparator.comparingDouble(Alimento::getProteina).reversed());

    map.put(new ComparatorKey("lip", Direction.asc), Comparator.comparingDouble(Alimento::getLipidos));
    map.put(new ComparatorKey("lip", Direction.desc), Comparator.comparingDouble(Alimento::getLipidos).reversed());

    map.put(new ComparatorKey("hc", Direction.asc), Comparator.comparingDouble(Alimento::getHidratosDeCarbono));
    map.put(new ComparatorKey("hc", Direction.desc),
        Comparator.comparingDouble(Alimento::getHidratosDeCarbono).reversed());
  }

  public static Comparator<Alimento> getComparator(String name, Direction dir) {
    return map.get(new ComparatorKey(name, dir));
  }

  private AlimentoComparators() {
  }
}
