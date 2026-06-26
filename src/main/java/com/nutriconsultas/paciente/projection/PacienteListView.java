package com.nutriconsultas.paciente.projection;

import java.util.Date;

import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Read-only projection for patient grid and search — excludes heavy TEXT and
 * energy-preference columns (#156 Phase A).
 */
public interface PacienteListView {

	Long getId();

	String getName();

	String getEmail();

	String getPhone();

	Date getDob();

	String getGender();

	String getResponsibleName();

	PacienteStatus getStatus();

	String getPatientAuthSub();

}
