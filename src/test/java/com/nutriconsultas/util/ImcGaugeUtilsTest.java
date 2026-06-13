package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.NivelPeso;

class ImcGaugeUtilsTest {

	@Test
	void markerPositionAtMinimumReturnsZero() {
		assertThat(ImcGaugeUtils.markerPosition(15.0)).isEqualTo(0.0);
	}

	@Test
	void markerPositionAtMaximumReturnsHundred() {
		assertThat(ImcGaugeUtils.markerPosition(40.0)).isEqualTo(100.0);
	}

	@Test
	void markerPositionClampsBelowMinimum() {
		assertThat(ImcGaugeUtils.markerPosition(10.0)).isEqualTo(0.0);
	}

	@Test
	void markerPositionClampsAboveMaximum() {
		assertThat(ImcGaugeUtils.markerPosition(50.0)).isEqualTo(100.0);
	}

	@Test
	void markerPositionAtMidpoint() {
		assertThat(ImcGaugeUtils.markerPosition(27.5)).isEqualTo(50.0);
	}

	@Test
	void markerPositionReturnsZeroForNull() {
		assertThat(ImcGaugeUtils.markerPosition(null)).isEqualTo(0.0);
	}

	@Test
	void calculateNivelPesoMatchesBackendThresholds() {
		assertThat(ImcGaugeUtils.calculateNivelPeso(17.0)).isEqualTo(NivelPeso.BAJO);
		assertThat(ImcGaugeUtils.calculateNivelPeso(18.5)).isEqualTo(NivelPeso.BAJO);
		assertThat(ImcGaugeUtils.calculateNivelPeso(20.0)).isEqualTo(NivelPeso.NORMAL);
		assertThat(ImcGaugeUtils.calculateNivelPeso(25.0)).isEqualTo(NivelPeso.NORMAL);
		assertThat(ImcGaugeUtils.calculateNivelPeso(26.0)).isEqualTo(NivelPeso.ALTO);
		assertThat(ImcGaugeUtils.calculateNivelPeso(30.0)).isEqualTo(NivelPeso.ALTO);
		assertThat(ImcGaugeUtils.calculateNivelPeso(31.0)).isEqualTo(NivelPeso.SOBREPESO);
	}

	@Test
	void resolveNivelPesoPrefersProvidedValue() {
		assertThat(ImcGaugeUtils.resolveNivelPeso(22.0, NivelPeso.ALTO)).isEqualTo(NivelPeso.ALTO);
	}

	@Test
	void resolveNivelPesoCalculatesWhenMissing() {
		assertThat(ImcGaugeUtils.resolveNivelPeso(22.0, null)).isEqualTo(NivelPeso.NORMAL);
	}

	@Test
	void markerColorReturnsExpectedHex() {
		assertThat(ImcGaugeUtils.markerColor(NivelPeso.BAJO)).isEqualTo("#3b82f6");
		assertThat(ImcGaugeUtils.markerColor(NivelPeso.NORMAL)).isEqualTo("#22c55e");
		assertThat(ImcGaugeUtils.markerColor(NivelPeso.ALTO)).isEqualTo("#eab308");
		assertThat(ImcGaugeUtils.markerColor(NivelPeso.SOBREPESO)).isEqualTo("#ef4444");
	}

	@Test
	void ariaLabelIncludesValueAndLevel() {
		assertThat(ImcGaugeUtils.ariaLabel(24.50, NivelPeso.NORMAL)).isEqualTo("IMC 24.50, nivel NORMAL");
	}

	@Test
	void ariaLabelHandlesNullImc() {
		assertThat(ImcGaugeUtils.ariaLabel(null, null)).isEqualTo("IMC no disponible");
	}

}
