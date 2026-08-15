package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.model.ApiResponse;

class DietaJsonSerializationTest {

	@Test
	void apiResponse_includesAlimentoMacrosWithoutIngestaNestingCycle() throws Exception {
		final Dieta dieta = new Dieta();
		dieta.setId(63L);
		dieta.setNombre("Dieta de prueba");

		final Ingesta ingesta = new Ingesta();
		ingesta.setId(249L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);

		final Alimento catalogAlimento = new Alimento();
		catalogAlimento.setId(10L);
		catalogAlimento.setCantSugerida(0.25);
		catalogAlimento.setUnidad("taza");

		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setId(51L);
		alimentoIngesta.setName("Amaranto tostado");
		alimentoIngesta.setPortions(1.0);
		alimentoIngesta.setEnergia(90);
		alimentoIngesta.setProteina(3.5);
		alimentoIngesta.setLipidos(1.5);
		alimentoIngesta.setHidratosDeCarbono(16.0);
		alimentoIngesta.setAlimento(catalogAlimento);
		alimentoIngesta.setIngesta(ingesta);

		final PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(255L);
		platilloIngesta.setName("Huevos");
		platilloIngesta.setPortions(1);
		platilloIngesta.setEnergia(150);
		platilloIngesta.setProteina(12.0);
		platilloIngesta.setIngesta(ingesta);

		ingesta.getAlimentos().add(alimentoIngesta);
		ingesta.getPlatillos().add(platilloIngesta);
		dieta.getIngestas().add(ingesta);

		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode json = mapper.readTree(mapper.writeValueAsString(new ApiResponse<>(dieta)));
		final JsonNode alimentoJson = json.get("data").get("ingestas").get(0).get("alimentos").get(0);
		final JsonNode platilloJson = json.get("data").get("ingestas").get(0).get("platillos").get(0);

		assertThat(alimentoJson.get("energia").asInt()).isEqualTo(90);
		assertThat(alimentoJson.get("proteina").asDouble()).isEqualTo(3.5);
		assertThat(alimentoJson.get("displayCantidad").asText()).isEqualTo("1/4");
		assertThat(alimentoJson.has("ingesta")).isFalse();
		assertThat(platilloJson.get("energia").asInt()).isEqualTo(150);
		assertThat(platilloJson.has("ingesta")).isFalse();
	}

}
