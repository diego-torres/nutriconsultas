package com.nutriconsultas.paciente;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class PacienteComparators {

	static Map<ComparatorKey, Comparator<Paciente>> map = new HashMap<>();

	static {
		map.put(new ComparatorKey("nombre", Direction.asc), Comparator.comparing(Paciente::getName));
		map.put(new ComparatorKey("nombre", Direction.desc), Comparator.comparing(Paciente::getName).reversed());

		map.put(new ComparatorKey("dob", Direction.asc), Comparator.comparing(Paciente::getDob));
		map.put(new ComparatorKey("dob", Direction.desc), Comparator.comparing(Paciente::getDob));

		// TODO: Add more sorting columns
	}

	public static Comparator<Paciente> getComparator(String name, Direction dir) {
		return map.get(new ComparatorKey(name, dir));
	}

	private PacienteComparators() {
	}

}
