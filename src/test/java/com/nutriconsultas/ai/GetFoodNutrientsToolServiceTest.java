package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

@ExtendWith(MockitoExtension.class)
class GetFoodNutrientsToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private GetFoodNutrientsToolServiceImpl service;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Test
	void getNutrientsReturnsCatalogDefaultsForSuggestedQuantity() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 0.5, 100, 80, 3.0, 2.0, 18.0, 1.5, 5.0, 150.0);
		when(alimentosRepository.findById(1L)).thenReturn(Optional.of(avena));

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "1/2", null, 1,
				"taza");

		assertThat(result.success()).isTrue();
		assertThat(result.data().alimentoId()).isEqualTo(1L);
		assertThat(result.data().nombreAlimento()).isEqualTo("Avena");
		assertThat(result.data().cantidad()).isEqualTo("1/2");
		assertThat(result.data().nutrientsPerCalculation().energiaKcal()).isEqualTo(100);
		assertThat(result.data().nutrientsPerCalculation().proteinaG()).isEqualTo(3.0);
		assertThat(result.data().nutrientsTotal().energiaKcal()).isEqualTo(100);
	}

	@Test
	void getNutrientsScalesByCustomQuantity() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 1.0, 200, 160, 6.0, 4.0, 36.0, 3.0, 10.0, 300.0);
		when(alimentosRepository.findById(1L)).thenReturn(Optional.of(avena));

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "1/2", null, 1, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrientsPerCalculation().energiaKcal()).isEqualTo(100);
		assertThat(result.data().nutrientsPerCalculation().proteinaG()).isEqualTo(3.0);
	}

	@Test
	void getNutrientsScalesByPesoNeto() {
		final Alimento pollo = sampleAlimento(2L, "Pechuga de pollo", "g", 1.0, 165, 100, 31.0, 3.6, 0.0, 0.0, 74.0,
				256.0);
		when(alimentosRepository.findById(2L)).thenReturn(Optional.of(pollo));

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 2L, "1", 150, 1, "g");

		assertThat(result.success()).isTrue();
		assertThat(result.data().pesoNetoG()).isEqualTo(150);
		assertThat(result.data().nutrientsPerCalculation().energiaKcal()).isEqualTo(248);
	}

	@Test
	void getNutrientsMultipliesTotalsByPortions() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 0.5, 100, 80, 3.0, 2.0, 18.0, 1.5, 5.0, 150.0);
		when(alimentosRepository.findById(1L)).thenReturn(Optional.of(avena));

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "1/2", null, 3,
				"taza");

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrientsPerCalculation().energiaKcal()).isEqualTo(100);
		assertThat(result.data().nutrientsTotal().energiaKcal()).isEqualTo(300);
		assertThat(result.data().nutrientsTotal().proteinaG()).isEqualTo(9.0);
	}

	@Test
	void getNutrientsReturnsNotFoundForMissingAlimento() {
		when(alimentosRepository.findById(99L)).thenReturn(Optional.empty());

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 99L, "1", null, 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

	@Test
	void getNutrientsRejectsUnsupportedUnit() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 0.5, 100, 80, 3.0, 2.0, 18.0, 1.5, 5.0, 150.0);
		when(alimentosRepository.findById(1L)).thenReturn(Optional.of(avena));

		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "1/2", null, 1, "g");

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void getNutrientsRejectsInvalidCantidad() {
		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "  ", null, 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void getNutrientsRejectsInvalidPortions() {
		final AiToolResult<FoodNutrientsData> result = service.getNutrients(NUTRITIONIST_ID, 1L, "1", null, 0, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	private static Alimento sampleAlimento(final long id, final String nombre, final String unidad,
			final double cantSugerida, final int energia, final int pesoNeto, final double proteina,
			final double lipidos, final double hidratos, final double fibra, final double sodio, final double potasio) {
		final Alimento alimento = new Alimento();
		alimento.setId(id);
		alimento.setNombreAlimento(nombre);
		alimento.setClasificacion("Cereales");
		alimento.setUnidad(unidad);
		alimento.setCantSugerida(cantSugerida);
		alimento.setEnergia(energia);
		alimento.setPesoNeto(pesoNeto);
		alimento.setProteina(proteina);
		alimento.setLipidos(lipidos);
		alimento.setHidratosDeCarbono(hidratos);
		alimento.setFibra(fibra);
		alimento.setSodio(sodio);
		alimento.setPotasio(potasio);
		return alimento;
	}

}
