package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;

class AlimentoIngestaNutrientScalerTest {

	@Test
	void copyScaled_appliesFractionalMultiplierToMacrosAndWeight() {
		final Alimento alimento = new Alimento();
		alimento.setEnergia(200);
		alimento.setProteina(20.0);
		alimento.setPesoNeto(80);
		alimento.setPesoBrutoRedondeado(90);

		final AlimentoIngesta target = new AlimentoIngesta();
		AlimentoIngestaNutrientScaler.copyScaled(alimento, target, 0.5);

		assertThat(target.getEnergia()).isEqualTo(100);
		assertThat(target.getProteina()).isEqualTo(10.0);
		assertThat(target.getPesoNeto()).isEqualTo(40);
		assertThat(target.getPesoBrutoRedondeado()).isEqualTo(45);
	}

}
