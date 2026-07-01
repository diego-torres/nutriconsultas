package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloRepository;

@ExtendWith(MockitoExtension.class)
class AiIngestaNutrientCalculatorTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiIngestaNutrientCalculator calculator;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Mock
	private PlatilloRepository platilloRepository;

	@Mock
	private CalculateRecipeNutrientsToolService recipeNutrientsToolService;

	@Test
	void computeIngestasSumsAlimentoItems() {
		final Alimento avena = sampleAlimento(1L, "Avena", "taza", 1.0, 200, 160, 6.0, 4.0, 36.0);
		when(alimentosRepository.findById(1L)).thenReturn(Optional.of(avena));

		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = calculator
			.computeIngestas(NUTRITIONIST_ID, List.of(new IngestaSlotInput("Desayuno", 1,
					List.of(new IngestaSlotItemInput("ALIMENTO", null, 1L, 2, null)))));

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrients().energiaKcal()).isEqualTo(400);
		assertThat(result.data().alimentoIds()).contains(1L);
	}

	@Test
	void computeIngestasUsesAuthorizedPlatillo() {
		final Platillo platillo = new Platillo();
		platillo.setId(10L);
		platillo.setName("Ensalada");
		platillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		platillo.setEnergia(150);
		platillo.setProteina(5.0);
		platillo.setLipidos(8.0);
		platillo.setHidratosDeCarbono(12.0);
		when(platilloRepository.findById(10L)).thenReturn(Optional.of(platillo));

		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = calculator
			.computeIngestas(NUTRITIONIST_ID, List.of(new IngestaSlotInput("Comida", 1,
					List.of(new IngestaSlotItemInput("PLATILLO", 10L, null, 2, null)))));

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrients().energiaKcal()).isEqualTo(300);
	}

	@Test
	void computeIngestasRejectsUnauthorizedPlatillo() {
		final Platillo platillo = new Platillo();
		platillo.setId(11L);
		platillo.setUserId("auth0|other");
		when(platilloRepository.findById(11L)).thenReturn(Optional.of(platillo));

		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = calculator
			.computeIngestas(NUTRITIONIST_ID, List.of(new IngestaSlotInput("Comida", 1,
					List.of(new IngestaSlotItemInput("PLATILLO", 11L, null, 1, null)))));

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

	@Test
	void computeDishDelegatesToRecipeService() {
		final RecipeNutrientsData recipeData = new RecipeNutrientsData(1,
				List.of(new RecipeIngredientNutrientResult(1L,
						new NutrientSummary(300, 20.0, 10.0, 30.0, 4.0, 200.0, 400.0), List.of())),
				new NutrientSummary(300, 20.0, 10.0, 30.0, 4.0, 200.0, 400.0),
				new NutrientSummary(300, 20.0, 10.0, 30.0, 4.0, 200.0, 400.0));
		when(recipeNutrientsToolService.calculate(any(), any(), any(), any()))
			.thenReturn(AiToolResult.success(recipeData));

		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = calculator.computeDish(
				NUTRITIONIST_ID, new DishPlanInput(List.of(new RecipeIngredientInput(1L, "1", null, null)), 1));

		assertThat(result.success()).isTrue();
		assertThat(result.data().nutrients().energiaKcal()).isEqualTo(300);
	}

	private static Alimento sampleAlimento(final long id, final String nombre, final String unidad,
			final double cantSugerida, final int energia, final int pesoNeto, final double proteina,
			final double lipidos, final double hidratos) {
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
		return alimento;
	}

}
