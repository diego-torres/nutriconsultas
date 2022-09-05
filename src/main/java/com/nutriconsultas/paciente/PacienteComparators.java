package com.nutriconsultas.paciente;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class PacienteComparators {
  static Map<ComparatorKey, Comparator<Paciente>> map = new HashMap<>();

  static {
    map.put(new ComparatorKey("name", Direction.asc), Comparator.comparing(Paciente::getName));
    map.put(new ComparatorKey("name", Direction.desc), Comparator.comparing(Paciente::getName).reversed());

    // TODO: Add more sorting columns
  }

  public static Comparator<Paciente> getComparator(String name, Direction dir) {
    return map.get(new ComparatorKey(name, dir));
  }

  private PacienteComparators() {
  }

}
