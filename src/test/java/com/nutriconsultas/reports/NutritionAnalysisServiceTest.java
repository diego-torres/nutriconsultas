package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class NutritionAnalysisServiceTest {

	@InjectMocks
	private NutritionAnalysisService nutritionAnalysisService;

	@Mock
	private DietaService dietaService;

	private Dieta dieta;

	private Ingesta ingesta;

	private PlatilloIngesta platilloIngesta;

	private static final String TEST_USER_ID = "test-user-id-123";

	private static final String OTHER_USER_ID = "other-user-id-456";

	@BeforeEach
	public void setup() {
		log.info("Setting up NutritionAnalysisService test");

		// Create dieta
		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");
		dieta.setUserId(TEST_USER_ID);
		dieta.setIngestas(new ArrayList<>());

		// Create ingesta
		ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Create platillo ingesta with low nutrients (deficiencies)
		platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(1L);
		platilloIngesta.setName("Platillo de prueba");
		platilloIngesta.setPortions(1);
		platilloIngesta.setEnergia(500);
		platilloIngesta.setProteina(10.0); // Below RDV (50g)
		platilloIngesta.setLipidos(20.0); // Below RDV (65g)
		platilloIngesta.setHidratosDeCarbono(100.0); // Below RDV (300g)
		platilloIngesta.setFibra(10.0); // Below RDV (25g)
		platilloIngesta.setVitA(200.0); // Below RDV (900μg)
		platilloIngesta.setAcidoAscorbico(30.0); // Below RDV (90mg)
		platilloIngesta.setAcidoFolico(100.0); // Below RDV (400μg)
		platilloIngesta.setCalcio(300.0); // Below RDV (1000mg)
		platilloIngesta.setHierro(5.0); // Below RDV (18mg)
		platilloIngesta.setPotasio(1000.0); // Below RDV (3500mg)
		platilloIngesta.setFosforo(200.0); // Below RDV (700mg)
		platilloIngesta.setSelenio(10.0); // Below RDV (55μg)
		platilloIngesta.setSodio(500.0); // Below UL (2300mg)
		platilloIngesta.setColesterol(100.0); // Below UL (300mg)
		platilloIngesta.setAgSaturados(5.0); // Below UL (20g)
		platilloIngesta.setAzucarPorEquivalente(20.0); // Below UL (50g)
		platilloIngesta.setIngesta(ingesta);

		ingesta.getPlatillos().add(platilloIngesta);
		dieta.getIngestas().add(ingesta);

		log.info("Finished setting up NutritionAnalysisService test");
	}

	@Test
	public void testAnalyzeDietWithDeficiencies() {
		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(1L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getDieta()).isNotNull();
		assertThat(result.getDieta().getId()).isEqualTo(1L);
		assertThat(result.getDieta().getNombre()).isEqualTo("Dieta de Prueba");
		assertThat(result.getTotals()).isNotNull();
		assertThat(result.getDeficiencies()).isNotEmpty();
		assertThat(result.getExcesses()).isEmpty();
		assertThat(result.getDistribution()).isNotNull();
		assertThat(result.getRecommendations()).isNotEmpty();

		// Verify totals
		assertThat(result.getTotals().getEnergia()).isEqualTo(500);
		assertThat(result.getTotals().getProteina()).isEqualTo(10.0);
		assertThat(result.getTotals().getLipidos()).isEqualTo(20.0);
		assertThat(result.getTotals().getHidratosDeCarbono()).isEqualTo(100.0);

		// Verify deficiencies exist
		assertThat(result.getDeficiencies().size()).isGreaterThan(0);
		final boolean hasProteinDeficiency = result.getDeficiencies()
			.stream()
			.anyMatch(d -> "Proteína".equals(d.getNutrientName()));
		assertThat(hasProteinDeficiency).isTrue();
	}

	@Test
	public void testAnalyzeDietWithExcesses() {
		// Create dieta with excess nutrients
		final Dieta dietaExcess = new Dieta();
		dietaExcess.setId(2L);
		dietaExcess.setNombre("Dieta con Excesos");
		dietaExcess.setUserId(TEST_USER_ID);
		dietaExcess.setIngestas(new ArrayList<>());

		final Ingesta ingestaExcess = new Ingesta();
		ingestaExcess.setId(2L);
		ingestaExcess.setNombre("Comida");
		ingestaExcess.setDieta(dietaExcess);
		ingestaExcess.setPlatillos(new ArrayList<>());
		ingestaExcess.setAlimentos(new ArrayList<>());

		final PlatilloIngesta platilloExcess = new PlatilloIngesta();
		platilloExcess.setId(2L);
		platilloExcess.setName("Platillo con excesos");
		platilloExcess.setSodio(3000.0); // Above UL (2300mg)
		platilloExcess.setColesterol(400.0); // Above UL (300mg)
		platilloExcess.setAgSaturados(30.0); // Above UL (20g)
		platilloExcess.setAzucarPorEquivalente(80.0); // Above UL (50g)
		platilloExcess.setIngesta(ingestaExcess);

		ingestaExcess.getPlatillos().add(platilloExcess);
		dietaExcess.getIngestas().add(ingestaExcess);

		when(dietaService.getDietaByIdAndUserId(2L, TEST_USER_ID)).thenReturn(dietaExcess);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(2L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getExcesses()).isNotEmpty();
		assertThat(result.getExcesses().size()).isGreaterThanOrEqualTo(4); // At least 4
																			// excesses
	}

	@Test
	public void testAnalyzeDietWithBalancedNutrients() {
		// Create dieta with balanced nutrients
		final Dieta dietaBalanced = new Dieta();
		dietaBalanced.setId(3L);
		dietaBalanced.setNombre("Dieta Balanceada");
		dietaBalanced.setUserId(TEST_USER_ID);
		dietaBalanced.setIngestas(new ArrayList<>());

		final Ingesta ingestaBalanced = new Ingesta();
		ingestaBalanced.setId(3L);
		ingestaBalanced.setNombre("Comida");
		ingestaBalanced.setDieta(dietaBalanced);
		ingestaBalanced.setPlatillos(new ArrayList<>());
		ingestaBalanced.setAlimentos(new ArrayList<>());

		final PlatilloIngesta platilloBalanced = new PlatilloIngesta();
		platilloBalanced.setId(3L);
		platilloBalanced.setName("Platillo balanceado");
		platilloBalanced.setEnergia(2000);
		platilloBalanced.setProteina(60.0); // Above RDV (50g)
		platilloBalanced.setLipidos(70.0); // Above RDV (65g)
		platilloBalanced.setHidratosDeCarbono(320.0); // Above RDV (300g)
		platilloBalanced.setFibra(30.0); // Above RDV (25g)
		platilloBalanced.setVitA(1000.0); // Above RDV (900μg)
		platilloBalanced.setAcidoAscorbico(100.0); // Above RDV (90mg)
		platilloBalanced.setAcidoFolico(450.0); // Above RDV (400μg)
		platilloBalanced.setCalcio(1100.0); // Above RDV (1000mg)
		platilloBalanced.setHierro(20.0); // Above RDV (18mg)
		platilloBalanced.setPotasio(3600.0); // Above RDV (3500mg)
		platilloBalanced.setFosforo(750.0); // Above RDV (700mg)
		platilloBalanced.setSelenio(60.0); // Above RDV (55μg)
		platilloBalanced.setSodio(2000.0); // Below UL (2300mg)
		platilloBalanced.setColesterol(250.0); // Below UL (300mg)
		platilloBalanced.setAgSaturados(15.0); // Below UL (20g)
		platilloBalanced.setAzucarPorEquivalente(40.0); // Below UL (50g)
		platilloBalanced.setIngesta(ingestaBalanced);

		ingestaBalanced.getPlatillos().add(platilloBalanced);
		dietaBalanced.getIngestas().add(ingestaBalanced);

		when(dietaService.getDietaByIdAndUserId(3L, TEST_USER_ID)).thenReturn(dietaBalanced);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(3L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getDeficiencies()).isEmpty();
		assertThat(result.getExcesses()).isEmpty();
		assertThat(result.getRecommendations()).isNotEmpty();
	}

	@Test
	public void testAnalyzeDietWithAlimentos() {
		// Add alimento to existing ingesta
		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setName("Manzana");
		alimento.setPortions(2);
		alimento.setEnergia(100);
		alimento.setProteina(0.5);
		alimento.setLipidos(0.3);
		alimento.setHidratosDeCarbono(25.0);
		alimento.setFibra(4.0);
		alimento.setVitA(50.0);
		alimento.setAcidoAscorbico(10.0);
		alimento.setCalcio(10.0);
		alimento.setHierro(0.3);
		alimento.setIngesta(ingesta);

		ingesta.getAlimentos().add(alimento);

		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(1L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getTotals()).isNotNull();
		// Verify that alimento nutrients are included
		assertThat(result.getTotals().getEnergia()).isEqualTo(600); // 500 + 100
		assertThat(result.getTotals().getProteina()).isEqualTo(10.5); // 10.0 + 0.5
	}

	@Test
	public void testAnalyzeDietNotFound() {
		when(dietaService.getDietaByIdAndUserId(999L, TEST_USER_ID)).thenReturn(null);

		assertThatThrownBy(() -> nutritionAnalysisService.analyzeDiet(999L, TEST_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not found");
	}

	@Test
	public void testAnalyzeDietAccessDenied() {
		when(dietaService.getDietaByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(null);

		assertThatThrownBy(() -> nutritionAnalysisService.analyzeDiet(1L, OTHER_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not found");
	}

	@Test
	public void testAnalyzeEmptyDiet() {
		final Dieta emptyDieta = new Dieta();
		emptyDieta.setId(4L);
		emptyDieta.setNombre("Dieta Vacía");
		emptyDieta.setUserId(TEST_USER_ID);
		emptyDieta.setIngestas(new ArrayList<>());

		when(dietaService.getDietaByIdAndUserId(4L, TEST_USER_ID)).thenReturn(emptyDieta);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(4L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getTotals()).isNotNull();
		assertThat(result.getTotals().getEnergia()).isNull();
		assertThat(result.getTotals().getProteina()).isNull();
	}

	@Test
	public void testMacroDistributionCalculation() {
		// Create dieta with known macro values for distribution test
		final Dieta dietaDist = new Dieta();
		dietaDist.setId(5L);
		dietaDist.setNombre("Dieta Distribución");
		dietaDist.setUserId(TEST_USER_ID);
		dietaDist.setIngestas(new ArrayList<>());

		final Ingesta ingestaDist = new Ingesta();
		ingestaDist.setId(5L);
		ingestaDist.setNombre("Comida");
		ingestaDist.setDieta(dietaDist);
		ingestaDist.setPlatillos(new ArrayList<>());
		ingestaDist.setAlimentos(new ArrayList<>());

		// Protein: 50g (50*4 = 200 kcal)
		// Lipids: 30g (30*9 = 270 kcal)
		// Carbs: 100g (100*4 = 400 kcal)
		// Total: 870 kcal
		// Distribution: Protein: 200/870 = 23%, Lipids: 270/870 = 31%, Carbs: 400/870 =
		// 46%
		final PlatilloIngesta platilloDist = new PlatilloIngesta();
		platilloDist.setId(5L);
		platilloDist.setName("Platillo distribución");
		platilloDist.setProteina(50.0);
		platilloDist.setLipidos(30.0);
		platilloDist.setHidratosDeCarbono(100.0);
		platilloDist.setIngesta(ingestaDist);

		ingestaDist.getPlatillos().add(platilloDist);
		dietaDist.getIngestas().add(ingestaDist);

		when(dietaService.getDietaByIdAndUserId(5L, TEST_USER_ID)).thenReturn(dietaDist);

		final NutritionAnalysisResult result = nutritionAnalysisService.analyzeDiet(5L, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getDistribution()).isNotNull();
		assertThat(result.getDistribution().getProteinPercentage()).isNotNull();
		assertThat(result.getDistribution().getLipidsPercentage()).isNotNull();
		assertThat(result.getDistribution().getCarbohydratesPercentage()).isNotNull();

		// Verify distribution percentages (with small tolerance for floating point)
		assertThat(result.getDistribution().getProteinPercentage()).isBetween(22.0, 24.0);
		assertThat(result.getDistribution().getLipidsPercentage()).isBetween(30.0, 32.0);
		assertThat(result.getDistribution().getCarbohydratesPercentage()).isBetween(45.0, 47.0);
	}

}
