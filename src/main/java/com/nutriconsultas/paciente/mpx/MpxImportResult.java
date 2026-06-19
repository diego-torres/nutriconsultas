package com.nutriconsultas.paciente.mpx;

/**
 * Result of a successful MPX import (#222).
 *
 * @param pacienteId saved patient primary key
 * @param duplicateWarning {@code true} when another patient with the same name and date
 * of birth already exists for the nutritionist
 */
public record MpxImportResult(Long pacienteId, boolean duplicateWarning) {
}
