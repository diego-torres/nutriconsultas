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

		MAP.put(new ComparatorKey("dob", Direction.asc),
				Comparator.comparing(Paciente::getDob, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("dob", Direction.desc),
				Comparator.comparing(Paciente::getDob, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("email", Direction.asc),
				Comparator.comparing(Paciente::getEmail, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("email", Direction.desc),
				Comparator.comparing(Paciente::getEmail, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("phone", Direction.asc),
				Comparator.comparing(Paciente::getPhone, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("phone", Direction.desc),
				Comparator.comparing(Paciente::getPhone, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("gender", Direction.asc),
				Comparator.comparing(Paciente::getGender, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("gender", Direction.desc),
				Comparator.comparing(Paciente::getGender, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("responsible", Direction.asc),
				Comparator.comparing(Paciente::getResponsibleName, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("responsible", Direction.desc),
				Comparator.comparing(Paciente::getResponsibleName, Comparator.nullsLast(Comparator.reverseOrder())));
	}

	public static Comparator<Paciente> getComparator(String name, Direction dir) {
		return MAP.get(new ComparatorKey(name, dir));
	}

	private PacienteComparators() {
	}

}
