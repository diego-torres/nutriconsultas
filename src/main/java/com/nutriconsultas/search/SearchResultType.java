package com.nutriconsultas.search;

public enum SearchResultType {

	PACIENTE("Paciente"), ALIMENTO("Alimento"), PLATILLO("Platillo"), CALENDAR_EVENT("Evento de Calendario"),
	CLINICAL_EXAM("Consulta");

	private final String displayName;

	SearchResultType(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
