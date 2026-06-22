package com.nutriconsultas.subscription.clinic;

/**
 * Patient row for director transfer UI (display only; never log names).
 */
public record ClinicPatientSummary(Long patientId, String displayLabel) {
}
