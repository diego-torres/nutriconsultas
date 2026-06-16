package com.nutriconsultas.paciente.projection;

import java.util.Date;

/**
 * Read-only projection for calendar patient preselect dropdowns — #156 Phase A.
 */
public interface PacienteCalendarView {

	Long getId();

	String getName();

	Date getDob();

	String getGender();

}
