package com.nutriconsultas.paciente;

/**
 * How a {@link PacienteDieta} binds menus to calendar time (#525).
 */
public enum PacienteDietaAssignmentType {

	/** Single diet for the whole assignment period (legacy default). */
	DATE_RANGE,

	/** One diet per weekday (lunes–domingo). */
	WEEKLY

}
