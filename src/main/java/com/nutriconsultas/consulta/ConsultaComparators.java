package com.nutriconsultas.consulta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class ConsultaComparators {
  static Map<ComparatorKey, Comparator<Consulta>> map = new HashMap<>();

  static {
    map.put(new ComparatorKey("fecha", Direction.asc), Comparator.comparing(Consulta::getFechaConsulta));
    map.put(new ComparatorKey("fecha", Direction.desc), Comparator.comparing(Consulta::getFechaConsulta).reversed());

    // TODO: Add more sorting columns
  }

  public static Comparator<Consulta> getComparator(String name, Direction dir) {
    return map.get(new ComparatorKey(name, dir));
  }

  private ConsultaComparators() {
  }
}
