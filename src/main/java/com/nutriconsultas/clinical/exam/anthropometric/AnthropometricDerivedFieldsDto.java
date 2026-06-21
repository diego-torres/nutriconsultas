package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Date;

import com.nutriconsultas.paciente.NivelPeso;

/**
 * Derived metrics returned after a field correction (#242).
 */
public record AnthropometricDerivedFieldsDto(Double fieldValue, Double imc, NivelPeso nivelPeso,
		Double porcentajeGrasaCorporal, Double porcentajeMasaMuscular, Double masaOseaKg, Double porcentajeMasaOsea,
		Double endomorphy, Double mesomorphy, Double ectomorphy, Double bmrUsed, Double getKcal, Double finalTotalKcal,
		Date updatedAt) {
}
