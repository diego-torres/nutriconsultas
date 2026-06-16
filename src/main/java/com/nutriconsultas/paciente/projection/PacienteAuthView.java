package com.nutriconsultas.paciente.projection;

/**
 * Read-only projection for mobile JWT resolution ({@code sub} → {@code patientAuthSub}) —
 * #156 Phase A.
 */
public interface PacienteAuthView {

	Long getId();

	String getPatientAuthSub();

	String getUserId();

}
