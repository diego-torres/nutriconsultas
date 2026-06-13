package com.nutriconsultas.util;

import com.nutriconsultas.paciente.NivelPeso;

/**
 * Utility for IMC gauge bar calculations used in Thymeleaf templates. Range is fixed at
 * 15–40 with {@link NivelPeso} thresholds matching backend logic.
 */
public final class ImcGaugeUtils {

	public static final double MIN_IMC = 15.0;

	public static final double MAX_IMC = 40.0;

	private static final double THRESHOLD_SOBREPESO = 30.0;

	private static final double THRESHOLD_ALTO = 25.0;

	private static final double THRESHOLD_NORMAL = 18.5;

	private ImcGaugeUtils() {
	}

	public static double markerPosition(final Double imc) {
		if (imc == null) {
			return 0.0;
		}
		final double clamped = Math.max(MIN_IMC, Math.min(MAX_IMC, imc));
		return ((clamped - MIN_IMC) / (MAX_IMC - MIN_IMC)) * 100.0;
	}

	public static NivelPeso resolveNivelPeso(final Double imc, final NivelPeso nivelPeso) {
		if (nivelPeso != null) {
			return nivelPeso;
		}
		return calculateNivelPeso(imc);
	}

	public static NivelPeso calculateNivelPeso(final Double imc) {
		if (imc == null) {
			return null;
		}
		if (imc > THRESHOLD_SOBREPESO) {
			return NivelPeso.SOBREPESO;
		}
		if (imc > THRESHOLD_ALTO) {
			return NivelPeso.ALTO;
		}
		if (imc > THRESHOLD_NORMAL) {
			return NivelPeso.NORMAL;
		}
		return NivelPeso.BAJO;
	}

	public static String markerColor(final NivelPeso nivelPeso) {
		if (nivelPeso == null) {
			return "#6c757d";
		}
		return switch (nivelPeso) {
			case BAJO -> "#3b82f6";
			case NORMAL -> "#22c55e";
			case ALTO -> "#eab308";
			case SOBREPESO -> "#ef4444";
		};
	}

	public static String ariaLabel(final Double imc, final NivelPeso nivelPeso) {
		final NivelPeso level = resolveNivelPeso(imc, nivelPeso);
		if (imc == null || level == null) {
			return "IMC no disponible";
		}
		return String.format("IMC %.2f, nivel %s", imc, level.name());
	}

}
