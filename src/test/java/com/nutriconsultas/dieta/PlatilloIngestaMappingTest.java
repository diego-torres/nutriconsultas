package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;

class PlatilloIngestaMappingTest {

	@Test
	void mapPlatilloIngestaCopiesMetadata() {
		final Platillo platillo = new Platillo();
		platillo.setName("Test platillo");
		platillo.setDescription("Desc");
		platillo.setEnergia(100);
		platillo.setProteina(10.0);

		final PlatilloIngesta ingesta = PlatilloIngestaMapping.mapPlatilloIngesta(platillo);

		assertThat(ingesta.getName()).isEqualTo("Test platillo");
		assertThat(ingesta.getRecommendations()).isEqualTo("Desc");
		assertThat(ingesta.getEnergia()).isEqualTo(100);
		assertThat(ingesta.getProteina()).isEqualTo(10.0);
	}

	@Test
	void mapIngredienteCopiesAlimentoLink() {
		final Alimento alimento = new Alimento();
		alimento.setId(42L);
		final Ingrediente ingrediente = new Ingrediente();
		ingrediente.setDescription("i");
		ingrediente.setAlimento(alimento);
		ingrediente.setEnergia(55);

		final IngredientePlatilloIngesta mapped = PlatilloIngestaMapping
			.mapFromIngredienteToIngredientePlatilloIngesta(ingrediente);

		assertThat(mapped.getDescription()).isEqualTo("i");
		assertThat(mapped.getAlimento().getId()).isEqualTo(42L);
		assertThat(mapped.getEnergia()).isEqualTo(55);
	}

}
