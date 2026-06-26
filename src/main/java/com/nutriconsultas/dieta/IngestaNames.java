package com.nutriconsultas.dieta;

import java.util.Collection;
import java.util.Locale;

public final class IngestaNames {

	private IngestaNames() {
	}

	public static String normalizeKey(final String nombre) {
		if (nombre == null) {
			return "";
		}
		return nombre.trim().toLowerCase(Locale.ROOT);
	}

	public static boolean isBlank(final String nombre) {
		return nombre == null || nombre.trim().isEmpty();
	}

	public static boolean hasDuplicateName(final Collection<Ingesta> ingestas, final String nombre,
			final Long excludeIngestaId) {
		final String key = normalizeKey(nombre);
		if (key.isEmpty()) {
			return false;
		}
		return ingestas.stream()
			.filter(ingesta -> excludeIngestaId == null || !excludeIngestaId.equals(ingesta.getId()))
			.anyMatch(ingesta -> key.equals(normalizeKey(ingesta.getNombre())));
	}

	public static void validateForDieta(final Collection<Ingesta> ingestas, final String nombre,
			final Long excludeIngestaId) {
		if (isBlank(nombre)) {
			throw new IllegalArgumentException("El nombre de la ingesta es requerido");
		}
		if (hasDuplicateName(ingestas, nombre, excludeIngestaId)) {
			throw new IllegalArgumentException("Ya existe una ingesta con ese nombre en este plan alimentario");
		}
	}

}
