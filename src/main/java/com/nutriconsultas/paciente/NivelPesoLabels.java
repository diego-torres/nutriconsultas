package com.nutriconsultas.paciente;

/**
 * Patient-facing display labels for {@link NivelPeso} (mobile progress snapshot).
 */
public final class NivelPesoLabels {

	private NivelPesoLabels() {
	}

	public static String toImcLabel(final NivelPeso nivelPeso) {
		if (nivelPeso == null) {
			return null;
		}
		return switch (nivelPeso) {
			case BAJO -> "Bajo peso";
			case NORMAL -> "Normal";
			case ALTO -> "Sobrepeso";
			case SOBREPESO -> "Obesidad";
		};
	}

}
