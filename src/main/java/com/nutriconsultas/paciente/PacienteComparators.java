package com.nutriconsultas.paciente;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;

public final class PacienteComparators {

	private static final Map<ComparatorKey, Comparator<Paciente>> MAP = new HashMap<>();

	static {
		MAP.put(new ComparatorKey("nombre", Direction.asc), Comparator.comparing(Paciente::getName));
		MAP.put(new ComparatorKey("nombre", Direction.desc), Comparator.comparing(Paciente::getName).reversed());

		MAP.put(new ComparatorKey("dob", Direction.asc), Comparator.comparing(Paciente::getDob));
		MAP.put(new ComparatorKey("dob", Direction.desc), Comparator.comparing(Paciente::getDob));

		// TODO: Add more sorting columns
	}

	public static Comparator<Paciente> getComparator(String name, Direction dir) {
		return MAP.get(new ComparatorKey(name, dir));
	}

	private PacienteComparators() {
	}

}
