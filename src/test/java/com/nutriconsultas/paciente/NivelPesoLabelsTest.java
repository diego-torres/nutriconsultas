package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NivelPesoLabelsTest {

	@Test
	void toImcLabel_mapsAllLevels() {
		assertThat(NivelPesoLabels.toImcLabel(NivelPeso.BAJO)).isEqualTo("Bajo peso");
		assertThat(NivelPesoLabels.toImcLabel(NivelPeso.NORMAL)).isEqualTo("Normal");
		assertThat(NivelPesoLabels.toImcLabel(NivelPeso.ALTO)).isEqualTo("Sobrepeso");
		assertThat(NivelPesoLabels.toImcLabel(NivelPeso.SOBREPESO)).isEqualTo("Obesidad");
		assertThat(NivelPesoLabels.toImcLabel(null)).isNull();
	}

}
