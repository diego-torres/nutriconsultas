package com.nutriconsultas.clinical.exam.anthropometric;

/**
 * Recalculation groups triggered when an anthropometric field is corrected (#242).
 *
 * <ul>
 * <li>{@link #BMI} — peso/talla → IMC and nivel de peso</li>
 * <li>{@link #COMPOSITION} — pliegues, bioimpedancia, manual % grasa, Deurenberg/Jackson
 * (#161)</li>
 * <li>{@link #SOMATOTYPE} — pliegues, perímetros, diámetros → Heath-Carter</li>
 * <li>{@link #ENERGY} — peso/talla + actividad → GET/TMB</li>
 * <li>{@link #PATIENT_SNAPSHOT} — sync patient peso/IMC when measurement is latest or
 * today</li>
 * </ul>
 */
public enum AnthropometricRecalcGroup {

	BMI, COMPOSITION, SOMATOTYPE, ENERGY, PATIENT_SNAPSHOT

}
