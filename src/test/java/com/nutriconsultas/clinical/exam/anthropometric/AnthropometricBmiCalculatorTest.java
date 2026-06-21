package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.NivelPeso;

@ActiveProfiles("test")
class AnthropometricBmiCalculatorTest {

	@Test
	void calculateImcReturnsExpectedValue() {
		assertThat(AnthropometricBmiCalculator.calculateImc(70.0, 1.75)).isEqualTo(22.857142857142858);
	}

	@Test
	void calculateNivelPesoMapsRanges() {
		assertThat(AnthropometricBmiCalculator.calculateNivelPeso(17.0)).isEqualTo(NivelPeso.BAJO);
		assertThat(AnthropometricBmiCalculator.calculateNivelPeso(22.0)).isEqualTo(NivelPeso.NORMAL);
		assertThat(AnthropometricBmiCalculator.calculateNivelPeso(27.0)).isEqualTo(NivelPeso.ALTO);
		assertThat(AnthropometricBmiCalculator.calculateNivelPeso(32.0)).isEqualTo(NivelPeso.SOBREPESO);
	}

}
