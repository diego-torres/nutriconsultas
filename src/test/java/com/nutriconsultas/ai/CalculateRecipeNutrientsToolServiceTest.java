package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

@ExtendWith(MockitoExtension.class)
class CalculateRecipeNutrientsToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private CalculateRecipeNutrientsToolServiceImpl service;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Test
	void calculateSumsIngredientsAndDividesPerPortion() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 1.0, 200, 160, 6.0, 4.0, 36.0, 3.0, 10.0, 300.0);
		final Alimento leche = sampleAlimento(2L, "Leche", "taza", 1.0, 120, 240, 8.0, 4.5, 12.0, 0.0, 105.0, 380.0);
		when(alimentosRepository.findAllById(any())).thenReturn(List.of(avena, leche));

		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID, List
			.of(new RecipeIngredientInput(1L, "1/2", null, "taza"), new RecipeIngredientInput(2L, "1/2", null, "taza")),
				2, "Avena con leche");

		assertThat(result.success()).isTrue();
		assertThat(result.data().portions()).isEqualTo(2);
		assertThat(result.data().ingredientResults()).hasSize(2);
		assertThat(result.data().nutrientsTotal().energiaKcal()).isEqualTo(160);
		assertThat(result.data().nutrientsPerPortion().energiaKcal()).isEqualTo(80);
		assertThat(result.data().nutrientsTotal().proteinaG()).isEqualTo(7.0);
	}

	@Test
	void calculateReturnsNotFoundForUnknownAlimento() {
		when(alimentosRepository.findAllById(any())).thenReturn(List.of());

		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID,
				List.of(new RecipeIngredientInput(99L, "1", null, null)), 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

	@Test
	void calculateRejectsUnsupportedUnit() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 1.0, 200, 160, 6.0, 4.0, 36.0, 3.0, 10.0, 300.0);
		when(alimentosRepository.findAllById(any())).thenReturn(List.of(avena));

		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID,
				List.of(new RecipeIngredientInput(1L, "1", null, "g")), 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void calculateIncludesMissingNutrientWarnings() {
		final Alimento incomplete = sampleAlimento(3L, "Agua", "taza", 1.0, 0, 240, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		when(alimentosRepository.findAllById(any())).thenReturn(List.of(incomplete));

		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID,
				List.of(new RecipeIngredientInput(3L, "1", null, null)), 1, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().ingredientResults().get(0).warnings()).isNotEmpty();
	}

	@Test
	void calculateRejectsEmptyIngredientList() {
		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID, List.of(), 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void calculateRejectsInvalidCantidad() {
		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID,
				List.of(new RecipeIngredientInput(1L, "  ", null, null)), 1, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void calculateScalesByPesoNeto() {
		final Alimento pollo = sampleAlimento(4L, "Pechuga de pollo", "g", 1.0, 165, 100, 31.0, 3.6, 0.0, 0.0, 74.0,
				256.0);
		when(alimentosRepository.findAllById(any())).thenReturn(List.of(pollo));

		final AiToolResult<RecipeNutrientsData> result = service.calculate(NUTRITIONIST_ID,
				List.of(new RecipeIngredientInput(4L, "1", 200, "g")), 1, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrientsTotal().energiaKcal()).isEqualTo(330);
		assertThat(result.data().nutrientsTotal().proteinaG()).isEqualTo(62.0);
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
