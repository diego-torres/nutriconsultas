package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class DietaServiceTest {

	@InjectMocks
	private DietaServiceImpl dietaService;

	@Mock
	private DietaRepository dietaRepository;

	private Dieta originalDieta;

	private Ingesta ingesta;

	private PlatilloIngesta platilloIngesta;

	private AlimentoIngesta alimentoIngesta;

	private IngredientePlatilloIngesta ingredientePlatilloIngesta;

	private static final String TEST_USER_ID = "test-user-id-123";

	private static final String OTHER_USER_ID = "other-user-id-456";

	@BeforeEach
	public void setup() {
		log.info("Setting up DietaService test");

		// Create original dieta
		originalDieta = new Dieta();
		originalDieta.setId(1L);
		originalDieta.setNombre("Dieta Original");
		originalDieta.setEnergia(2000);
		originalDieta.setProteina(100.0);
		originalDieta.setLipidos(50.0);
		originalDieta.setHidratosDeCarbono(200.0);
		originalDieta.setUserId(TEST_USER_ID);
		originalDieta.setIngestas(new ArrayList<>());

		// Create ingesta
		ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(originalDieta);
		ingesta.setEnergia(500);
		ingesta.setProteina(25.0);
		ingesta.setLipidos(12.0);
		ingesta.setHidratosDeCarbono(50.0);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Create platillo ingesta
		platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(1L);
		platilloIngesta.setName("Platillo de prueba");
		platilloIngesta.setPortions(2);
		platilloIngesta.setRecommendations("Recomendaciones");
		platilloIngesta.setImageUrl("image.jpg");
		platilloIngesta.setVideoUrl("video.mp4");
		platilloIngesta.setPdfUrl("pdf.pdf");
		platilloIngesta.setEnergia(300);
		platilloIngesta.setProteina(20.0);
		platilloIngesta.setLipidos(10.0);
		platilloIngesta.setHidratosDeCarbono(30.0);
		platilloIngesta.setPesoBrutoRedondeado(200);
		platilloIngesta.setPesoNeto(150);
		platilloIngesta.setFibra(5.0);
		platilloIngesta.setVitA(100.0);
		platilloIngesta.setAcidoAscorbico(50.0);
		platilloIngesta.setHierroNoHem(2.0);
		platilloIngesta.setPotasio(300.0);
		platilloIngesta.setIndiceGlicemico(50.0);
		platilloIngesta.setCargaGlicemica(25.0);
		platilloIngesta.setAcidoFolico(100.0);
		platilloIngesta.setCalcio(200.0);
		platilloIngesta.setHierro(3.0);
		platilloIngesta.setSodio(500.0);
		platilloIngesta.setAzucarPorEquivalente(10.0);
		platilloIngesta.setSelenio(20.0);
		platilloIngesta.setFosforo(150.0);
		platilloIngesta.setColesterol(30.0);
		platilloIngesta.setAgSaturados(5.0);
		platilloIngesta.setAgMonoinsaturados(3.0);
		platilloIngesta.setAgPoliinsaturados(2.0);
		platilloIngesta.setEtanol(0.0);
		platilloIngesta.setIngesta(ingesta);
		platilloIngesta.setIngredientes(new ArrayList<>());

		// Create ingrediente platillo ingesta
		Alimento alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Pollo");

		ingredientePlatilloIngesta = new IngredientePlatilloIngesta();
		ingredientePlatilloIngesta.setId(1L);
		ingredientePlatilloIngesta.setDescription("Pollo asado");
		ingredientePlatilloIngesta.setCantSugerida(1.5);
		ingredientePlatilloIngesta.setAlimento(alimento);
		ingredientePlatilloIngesta.setUnidad("pieza");
		ingredientePlatilloIngesta.setEnergia(200);
		ingredientePlatilloIngesta.setProteina(25.0);
		ingredientePlatilloIngesta.setLipidos(10.0);
		ingredientePlatilloIngesta.setHidratosDeCarbono(0.0);
		ingredientePlatilloIngesta.setPesoBrutoRedondeado(150);
		ingredientePlatilloIngesta.setPesoNeto(120);
		ingredientePlatilloIngesta.setFibra(0.0);
		ingredientePlatilloIngesta.setVitA(0.0);
		ingredientePlatilloIngesta.setAcidoAscorbico(0.0);
		ingredientePlatilloIngesta.setHierroNoHem(1.5);
		ingredientePlatilloIngesta.setPotasio(200.0);
		ingredientePlatilloIngesta.setIndiceGlicemico(0.0);
		ingredientePlatilloIngesta.setCargaGlicemica(0.0);
		ingredientePlatilloIngesta.setAcidoFolico(50.0);
		ingredientePlatilloIngesta.setCalcio(10.0);
		ingredientePlatilloIngesta.setHierro(2.0);
		ingredientePlatilloIngesta.setSodio(300.0);
		ingredientePlatilloIngesta.setAzucarPorEquivalente(0.0);
		ingredientePlatilloIngesta.setSelenio(15.0);
		ingredientePlatilloIngesta.setFosforo(100.0);
		ingredientePlatilloIngesta.setColesterol(50.0);
		ingredientePlatilloIngesta.setAgSaturados(3.0);
		ingredientePlatilloIngesta.setAgMonoinsaturados(4.0);
		ingredientePlatilloIngesta.setAgPoliinsaturados(3.0);
		ingredientePlatilloIngesta.setEtanol(0.0);
		ingredientePlatilloIngesta.setPlatillo(platilloIngesta);

		platilloIngesta.getIngredientes().add(ingredientePlatilloIngesta);
		ingesta.getPlatillos().add(platilloIngesta);

		// Create alimento ingesta
		alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setId(1L);
		alimentoIngesta.setName("Manzana");
		alimentoIngesta.setPortions(2);
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setUnidad("pieza");
		alimentoIngesta.setPesoBrutoRedondeado(200);
		alimentoIngesta.setPesoNeto(180);
		alimentoIngesta.setEnergia(100);
		alimentoIngesta.setProteina(0.5);
		alimentoIngesta.setLipidos(0.3);
		alimentoIngesta.setHidratosDeCarbono(25.0);
		alimentoIngesta.setFibra(4.0);
		alimentoIngesta.setVitA(50.0);
		alimentoIngesta.setAcidoAscorbico(10.0);
		alimentoIngesta.setHierroNoHem(0.5);
		alimentoIngesta.setPotasio(150.0);
		alimentoIngesta.setIndiceGlicemico(38.0);
		alimentoIngesta.setCargaGlicemica(9.5);
		alimentoIngesta.setAcidoFolico(5.0);
		alimentoIngesta.setCalcio(10.0);
		alimentoIngesta.setHierro(0.3);
		alimentoIngesta.setSodio(2.0);
		alimentoIngesta.setAzucarPorEquivalente(19.0);
		alimentoIngesta.setSelenio(0.1);
		alimentoIngesta.setFosforo(12.0);
		alimentoIngesta.setColesterol(0.0);
		alimentoIngesta.setAgSaturados(0.1);
		alimentoIngesta.setAgMonoinsaturados(0.05);
		alimentoIngesta.setAgPoliinsaturados(0.05);
		alimentoIngesta.setEtanol(0.0);
		alimentoIngesta.setIngesta(ingesta);

		ingesta.getAlimentos().add(alimentoIngesta);
		originalDieta.getIngestas().add(ingesta);

		log.info("Finished setting up DietaService test");
	}

	@Test
	public void testDuplicateDietaSuccess() {
		log.info("Starting testDuplicateDietaSuccess");

		// Arrange
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(originalDieta));
		when(dietaRepository.save(any(Dieta.class))).thenAnswer(invocation -> {
			Dieta saved = invocation.getArgument(0);
			saved.setId(2L);
			return saved;
		});

		// Act
		Dieta duplicated = dietaService.duplicateDieta(1L, TEST_USER_ID);

		// Assert
		assertThat(duplicated).isNotNull();
		assertThat(duplicated.getId()).isEqualTo(2L);
		assertThat(duplicated.getNombre()).isEqualTo("Copia de Dieta Original");
		assertThat(duplicated.getUserId()).isEqualTo(TEST_USER_ID);
		assertThat(duplicated.getEnergia()).isEqualTo(originalDieta.getEnergia());
		assertThat(duplicated.getProteina()).isEqualTo(originalDieta.getProteina());
		assertThat(duplicated.getLipidos()).isEqualTo(originalDieta.getLipidos());
		assertThat(duplicated.getHidratosDeCarbono()).isEqualTo(originalDieta.getHidratosDeCarbono());

		// Verify ingestas were copied
		assertThat(duplicated.getIngestas()).hasSize(1);
		Ingesta duplicatedIngesta = duplicated.getIngestas().get(0);
		assertThat(duplicatedIngesta.getNombre()).isEqualTo("Desayuno");
		assertThat(duplicatedIngesta.getEnergia()).isEqualTo(ingesta.getEnergia());
		assertThat(duplicatedIngesta.getProteina()).isEqualTo(ingesta.getProteina());
		assertThat(duplicatedIngesta.getLipidos()).isEqualTo(ingesta.getLipidos());
		assertThat(duplicatedIngesta.getHidratosDeCarbono()).isEqualTo(ingesta.getHidratosDeCarbono());
		assertThat(duplicatedIngesta.getDieta()).isEqualTo(duplicated);

		// Verify platillos were copied
		assertThat(duplicatedIngesta.getPlatillos()).hasSize(1);
		PlatilloIngesta duplicatedPlatillo = duplicatedIngesta.getPlatillos().get(0);
		assertThat(duplicatedPlatillo.getName()).isEqualTo(platilloIngesta.getName());
		assertThat(duplicatedPlatillo.getPortions()).isEqualTo(platilloIngesta.getPortions());
		assertThat(duplicatedPlatillo.getRecommendations()).isEqualTo(platilloIngesta.getRecommendations());
		assertThat(duplicatedPlatillo.getImageUrl()).isEqualTo(platilloIngesta.getImageUrl());
		assertThat(duplicatedPlatillo.getVideoUrl()).isEqualTo(platilloIngesta.getVideoUrl());
		assertThat(duplicatedPlatillo.getPdfUrl()).isEqualTo(platilloIngesta.getPdfUrl());
		assertThat(duplicatedPlatillo.getEnergia()).isEqualTo(platilloIngesta.getEnergia());
		assertThat(duplicatedPlatillo.getProteina()).isEqualTo(platilloIngesta.getProteina());
		assertThat(duplicatedPlatillo.getIngesta()).isEqualTo(duplicatedIngesta);

		// Verify ingredientes were copied
		assertThat(duplicatedPlatillo.getIngredientes()).hasSize(1);
		IngredientePlatilloIngesta duplicatedIngrediente = duplicatedPlatillo.getIngredientes().get(0);
		assertThat(duplicatedIngrediente.getDescription()).isEqualTo(ingredientePlatilloIngesta.getDescription());
		assertThat(duplicatedIngrediente.getCantSugerida()).isEqualTo(ingredientePlatilloIngesta.getCantSugerida());
		assertThat(duplicatedIngrediente.getAlimento()).isEqualTo(ingredientePlatilloIngesta.getAlimento());
		assertThat(duplicatedIngrediente.getUnidad()).isEqualTo(ingredientePlatilloIngesta.getUnidad());
		assertThat(duplicatedIngrediente.getPlatillo()).isEqualTo(duplicatedPlatillo);

		// Verify alimentos were copied
		assertThat(duplicatedIngesta.getAlimentos()).hasSize(1);
		AlimentoIngesta duplicatedAlimento = duplicatedIngesta.getAlimentos().get(0);
		assertThat(duplicatedAlimento.getName()).isEqualTo(alimentoIngesta.getName());
		assertThat(duplicatedAlimento.getPortions()).isEqualTo(alimentoIngesta.getPortions());
		assertThat(duplicatedAlimento.getAlimento()).isEqualTo(alimentoIngesta.getAlimento());
		assertThat(duplicatedAlimento.getUnidad()).isEqualTo(alimentoIngesta.getUnidad());
		assertThat(duplicatedAlimento.getEnergia()).isEqualTo(alimentoIngesta.getEnergia());
		assertThat(duplicatedAlimento.getProteina()).isEqualTo(alimentoIngesta.getProteina());
		assertThat(duplicatedAlimento.getIngesta()).isEqualTo(duplicatedIngesta);

		// Verify original dieta was not modified
		assertThat(originalDieta.getId()).isEqualTo(1L);
		assertThat(originalDieta.getNombre()).isEqualTo("Dieta Original");

		verify(dietaRepository).save(any(Dieta.class));
		log.info("Finishing testDuplicateDietaSuccess");
	}

	@Test
	public void testDuplicateDietaNotFound() {
		log.info("Starting testDuplicateDietaNotFound");

		// Arrange
		when(dietaRepository.findById(999L)).thenReturn(Optional.empty());

		// Act
		Dieta duplicated = dietaService.duplicateDieta(999L, TEST_USER_ID);

		// Assert
		assertThat(duplicated).isNull();
		log.info("Finishing testDuplicateDietaNotFound");
	}

	@Test
	public void testDuplicateDietaWithNullName() {
		log.info("Starting testDuplicateDietaWithNullName");

		// Arrange
		originalDieta.setNombre(null);
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(originalDieta));
		when(dietaRepository.save(any(Dieta.class))).thenAnswer(invocation -> {
			Dieta saved = invocation.getArgument(0);
			saved.setId(2L);
			return saved;
		});

		// Act
		Dieta duplicated = dietaService.duplicateDieta(1L, TEST_USER_ID);

		// Assert
		assertThat(duplicated).isNotNull();
		assertThat(duplicated.getNombre()).isEqualTo("Copia de Dieta");
		assertThat(duplicated.getUserId()).isEqualTo(TEST_USER_ID);
		log.info("Finishing testDuplicateDietaWithNullName");
	}

	@Test
	public void testGetDietaByIdAndUserIdSuccess() {
		log.info("Starting testGetDietaByIdAndUserIdSuccess");

		// Arrange
		when(dietaRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(originalDieta));

		// Act
		Dieta result = dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
		verify(dietaRepository).findByIdAndUserId(1L, TEST_USER_ID);
		log.info("Finishing testGetDietaByIdAndUserIdSuccess");
	}

	@Test
	public void testGetDietaByIdAndUserIdNotFound() {
		log.info("Starting testGetDietaByIdAndUserIdNotFound");

		// Arrange
		when(dietaRepository.findByIdAndUserId(999L, TEST_USER_ID)).thenReturn(Optional.empty());

		// Act
		Dieta result = dietaService.getDietaByIdAndUserId(999L, TEST_USER_ID);

		// Assert
		assertThat(result).isNull();
		verify(dietaRepository).findByIdAndUserId(999L, TEST_USER_ID);
		log.info("Finishing testGetDietaByIdAndUserIdNotFound");
	}

	@Test
	public void testGetDietaByIdAndUserIdWrongUser() {
		log.info("Starting testGetDietaByIdAndUserIdWrongUser");

		// Arrange - dieta exists but belongs to different user
		when(dietaRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

		// Act
		Dieta result = dietaService.getDietaByIdAndUserId(1L, OTHER_USER_ID);

		// Assert
		assertThat(result).isNull();
		verify(dietaRepository).findByIdAndUserId(1L, OTHER_USER_ID);
		log.info("Finishing testGetDietaByIdAndUserIdWrongUser");
	}

	@Test
	public void testDeleteDietaByIdAndUserIdSuccess() {
		log.info("Starting testDeleteDietaByIdAndUserIdSuccess");

		// Arrange
		when(dietaRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(originalDieta));

		// Act
		dietaService.deleteDietaByIdAndUserId(1L, TEST_USER_ID);

		// Assert
		verify(dietaRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).delete(originalDieta);
		log.info("Finishing testDeleteDietaByIdAndUserIdSuccess");
	}

	@Test
	public void testDeleteDietaByIdAndUserIdNotFound() {
		log.info("Starting testDeleteDietaByIdAndUserIdNotFound");

		// Arrange
		when(dietaRepository.findByIdAndUserId(999L, TEST_USER_ID)).thenReturn(Optional.empty());

		// Act
		dietaService.deleteDietaByIdAndUserId(999L, TEST_USER_ID);

		// Assert
		verify(dietaRepository).findByIdAndUserId(999L, TEST_USER_ID);
		verify(dietaRepository, never()).delete(any(Dieta.class));
		log.info("Finishing testDeleteDietaByIdAndUserIdNotFound");
	}

	@Test
	public void testDeleteDietaByIdAndUserIdWrongUser() {
		log.info("Starting testDeleteDietaByIdAndUserIdWrongUser");

		// Arrange - dieta exists but belongs to different user
		when(dietaRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

		// Act
		dietaService.deleteDietaByIdAndUserId(1L, OTHER_USER_ID);

		// Assert
		verify(dietaRepository).findByIdAndUserId(1L, OTHER_USER_ID);
		verify(dietaRepository, never()).delete(any(Dieta.class));
		log.info("Finishing testDeleteDietaByIdAndUserIdWrongUser");
	}

}
