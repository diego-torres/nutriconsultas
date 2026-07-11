package com.nutriconsultas.dieta;

import org.springframework.util.StringUtils;

public final class AlimentoPortionDefaults {

	public static final String PORCION = "porcion";

	public static final String GRAMOS = "gramos";

	private AlimentoPortionDefaults() {
	}

	public static String resolveTipoPorcion(final String tipoPorcion) {
		if (StringUtils.hasText(tipoPorcion)) {
			return tipoPorcion.trim();
		}
		return PORCION;
	}

}
