package com.nutriconsultas.paciente;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.nutriconsultas.dataTables.paging.ComparatorKey;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.paciente.projection.PacienteListView;

public final class PacienteComparators {

	private static final Map<ComparatorKey, Comparator<PacienteListView>> MAP = new HashMap<>();

	static {
		MAP.put(new ComparatorKey("nombre", Direction.asc), Comparator.comparing(PacienteListView::getName));
		MAP.put(new ComparatorKey("nombre", Direction.desc),
				Comparator.comparing(PacienteListView::getName).reversed());

		MAP.put(new ComparatorKey("dob", Direction.asc),
				Comparator.comparing(PacienteListView::getDob, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("dob", Direction.desc),
				Comparator.comparing(PacienteListView::getDob, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("email", Direction.asc),
				Comparator.comparing(PacienteListView::getEmail, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("email", Direction.desc),
				Comparator.comparing(PacienteListView::getEmail, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("phone", Direction.asc),
				Comparator.comparing(PacienteListView::getPhone, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("phone", Direction.desc),
				Comparator.comparing(PacienteListView::getPhone, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("gender", Direction.asc),
				Comparator.comparing(PacienteListView::getGender, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("gender", Direction.desc),
				Comparator.comparing(PacienteListView::getGender, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("responsible", Direction.asc), Comparator
			.comparing(PacienteListView::getResponsibleName, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("responsible", Direction.desc), Comparator
			.comparing(PacienteListView::getResponsibleName, Comparator.nullsLast(Comparator.reverseOrder())));

		MAP.put(new ComparatorKey("mobileApp", Direction.asc),
				Comparator.comparing(PacienteListView::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
		MAP.put(new ComparatorKey("mobileApp", Direction.desc),
				Comparator.comparing(PacienteListView::getStatus, Comparator.nullsLast(Comparator.reverseOrder())));
	}

	public static Comparator<PacienteListView> getComparator(String name, Direction dir) {
		return MAP.get(new ComparatorKey(name, dir));
	}

	private PacienteComparators() {
	}

}
