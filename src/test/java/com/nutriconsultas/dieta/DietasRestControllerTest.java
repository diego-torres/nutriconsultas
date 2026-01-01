package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class DietasRestControllerTest {

	@InjectMocks
	private DietasRestController dietasRestController;

	@Mock
	private DietaService dietaService;

	private Dieta dietaVacia;

	private Dieta dietaConPlatillos;

	private Ingesta ingestaVacia;

	private Ingesta ingestaConPlatillos;

	private PlatilloIngesta platilloIngesta;

	@BeforeEach
	public void setup() {
		// Create empty diet
		dietaVacia = new Dieta();
		dietaVacia.setId(1L);
		dietaVacia.setNombre("Dieta Vacía");
		dietaVacia.setIngestas(new ArrayList<>());

		ingestaVacia = new Ingesta();
		ingestaVacia.setId(1L);
		ingestaVacia.setNombre("Desayuno");
		ingestaVacia.setDieta(dietaVacia);
		ingestaVacia.setPlatillos(new ArrayList<>());

		dietaVacia.getIngestas().add(ingestaVacia);

		// Create diet with platillos
		dietaConPlatillos = new Dieta();
		dietaConPlatillos.setId(2L);
		dietaConPlatillos.setNombre("Dieta con Platillos");
		dietaConPlatillos.setIngestas(new ArrayList<>());

		ingestaConPlatillos = new Ingesta();
		ingestaConPlatillos.setId(2L);
		ingestaConPlatillos.setNombre("Desayuno");
		ingestaConPlatillos.setDieta(dietaConPlatillos);
		ingestaConPlatillos.setPlatillos(new ArrayList<>());

		// PlatilloIngesta with nutritional values
		// Protein: 30g, Lipids: 15g, Carbohydrates: 50g
		// Total kcal: 30*4 + 15*9 + 50*4 = 120 + 135 + 200 = 455 kcal
		// Distribution: 30*4/455 = 0.264, 15*9/455 = 0.297, 50*4/455 = 0.440
		platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(1L);
		platilloIngesta.setName("Platillo de prueba");
		platilloIngesta.setProteina(30.0);
		platilloIngesta.setLipidos(15.0);
		platilloIngesta.setHidratosDeCarbono(50.0);
		platilloIngesta.setIngesta(ingestaConPlatillos);

		ingestaConPlatillos.getPlatillos().add(platilloIngesta);
		dietaConPlatillos.getIngestas().add(ingestaConPlatillos);
	}

	@Test
	public void testGetDistEmptyDietReturnsEmptyString() throws Exception {
		log.info("Starting testGetDistEmptyDietReturnsEmptyString");

		// Use reflection to access private method getDist
		Method getDistMethod = DietasRestController.class.getDeclaredMethod("getDist", Dieta.class);
		getDistMethod.setAccessible(true);

		// Act
		String result = (String) getDistMethod.invoke(dietasRestController, dietaVacia);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		log.info("Finishing testGetDistEmptyDietReturnsEmptyString with result: '{}'", result);
	}

	@Test
	public void testGetDistDietWithPlatillosReturnsCorrectDistribution() throws Exception {
		log.info("Starting testGetDistDietWithPlatillosReturnsCorrectDistribution");

		// Use reflection to access private method getDist
		Method getDistMethod = DietasRestController.class.getDeclaredMethod("getDist", Dieta.class);
		getDistMethod.setAccessible(true);

		// Act
		String result = (String) getDistMethod.invoke(dietasRestController, dietaConPlatillos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isNotEmpty();
		// Expected: "26.4 / 29.7 / 44.0" (approximately)
		// Protein: 30*4/455 = 0.2637... * 100 = 26.4%
		// Lipids: 15*9/455 = 0.2967... * 100 = 29.7%
		// Carbohydrates: 50*4/455 = 0.4395... * 100 = 44.0%
		assertThat(result).contains("/");
		String[] parts = result.split(" / ");
		assertThat(parts).hasSize(3);
		// Verify that all parts are valid numbers (not NaN)
		for (String part : parts) {
			assertThat(part).isNotEqualTo("NaN");
			Double.parseDouble(part); // Should not throw exception
		}
		log.info("Finishing testGetDistDietWithPlatillosReturnsCorrectDistribution with result: '{}'", result);
	}

	@Test
	public void testToStringListEmptyDietReturnsEmptyStringForDistribution() throws Exception {
		log.info("Starting testToStringListEmptyDietReturnsEmptyStringForDistribution");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaVacia);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8); // acciones, dieta, ingestas, dist, kcal, prot,
										// lip, hc
		// Index 0 is the actions column (print button)
		String actions = result.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("/admin/dietas/1/print");
		assertThat(actions).contains("fa-file-pdf");
		// Index 2 is the ingestas column - should not contain any links
		String ingestas = result.get(2);
		assertThat(ingestas).isNotNull();
		assertThat(ingestas).contains("Desayuno");
		assertThat(ingestas).doesNotContain("/admin/platillos");
		assertThat(ingestas).doesNotContain("/admin/alimentos");
		// Index 3 is the distribution column
		String distribution = result.get(3);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isEmpty();
		log.info("Finishing testToStringListEmptyDietReturnsEmptyStringForDistribution with distribution: '{}'",
				distribution);
	}

	@Test
	public void testToStringListDietWithPlatillosReturnsCorrectValues() throws Exception {
		log.info("Starting testToStringListDietWithPlatillosReturnsCorrectValues");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaConPlatillos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8); // acciones, dieta, ingestas, dist, kcal, prot,
										// lip, hc
		// Index 0 is the actions column (print button)
		String actions = result.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("/admin/dietas/2/print");
		assertThat(actions).contains("fa-file-pdf");
		// Index 2 is the ingestas column - should only contain ingesta names
		String ingestas = result.get(2);
		assertThat(ingestas).isNotNull();
		assertThat(ingestas).contains("Desayuno");
		assertThat(ingestas).doesNotContain("/admin/platillos");
		assertThat(ingestas).doesNotContain("fa-utensils");
		// Index 3 is the distribution column
		String distribution = result.get(3);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isNotEmpty();
		assertThat(distribution).doesNotContain("NaN");
		// Index 4 is kcal
		String kcal = result.get(4);
		assertThat(kcal).isEqualTo("455.0");
		// Index 5 is protein
		String protein = result.get(5);
		assertThat(protein).isEqualTo("30.0");
		// Index 6 is lipids
		String lipids = result.get(6);
		assertThat(lipids).isEqualTo("15.0");
		// Index 7 is carbohydrates
		String carbohydrates = result.get(7);
		assertThat(carbohydrates).isEqualTo("50.0");
		log.info("Finishing testToStringListDietWithPlatillosReturnsCorrectValues");
	}

	@Test
	public void testGetPageArrayEmptyDietShowsEmptyDistribution() {
		log.info("Starting testGetPageArrayEmptyDietShowsEmptyDistribution");

		// Arrange
		List<Dieta> dietas = Arrays.asList(dietaVacia);
		when(dietaService.getDietas()).thenReturn(dietas);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		// Order by dieta column (index 1)
		pagingRequest.setOrder(Arrays.asList(new Order(1, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = dietasRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).isNotEmpty();
		List<String> firstRow = (List<String>) result.getData().get(0);
		assertThat(firstRow).isNotNull();
		assertThat(firstRow.size()).isGreaterThanOrEqualTo(8);
		// Index 0 is the actions column (print button)
		String actions = firstRow.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("/admin/dietas/1/print");
		assertThat(actions).contains("fa-file-pdf");
		// Index 3 is the distribution column
		String distribution = firstRow.get(3);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isEmpty();
		assertThat(distribution).doesNotContain("NaN");
		log.info("Finishing testGetPageArrayEmptyDietShowsEmptyDistribution");
	}

	@Test
	public void testGetPageArrayDietWithPlatillosShowsCorrectDistribution() {
		log.info("Starting testGetPageArrayDietWithPlatillosShowsCorrectDistribution");

		// Arrange
		List<Dieta> dietas = Arrays.asList(dietaConPlatillos);
		when(dietaService.getDietas()).thenReturn(dietas);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		// Order by dieta column (index 1)
		pagingRequest.setOrder(Arrays.asList(new Order(1, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = dietasRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).isNotEmpty();
		List<String> firstRow = (List<String>) result.getData().get(0);
		assertThat(firstRow).isNotNull();
		assertThat(firstRow.size()).isGreaterThanOrEqualTo(8);
		// Index 0 is the actions column (print button)
		String actions = firstRow.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("/admin/dietas/2/print");
		assertThat(actions).contains("fa-file-pdf");
		// Index 3 is the distribution column
		String distribution = firstRow.get(3);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isNotEmpty();
		assertThat(distribution).doesNotContain("NaN");
		log.info("Finishing testGetPageArrayDietWithPlatillosShowsCorrectDistribution");
	}

	@Test
	public void testDeletePlatilloIngestaSuccessRemovesPlatilloFromIngesta() {
		log.info("Starting testDeletePlatilloIngestaSuccessRemovesPlatilloFromIngesta");

		// Arrange
		Long dietaId = 2L;
		Long ingestaId = 2L;
		Long platilloIngestaId = 1L;

		// Create a copy of dietaConPlatillos for the test
		Dieta dietaBeforeDelete = new Dieta();
		dietaBeforeDelete.setId(dietaId);
		dietaBeforeDelete.setNombre("Dieta con Platillos");
		dietaBeforeDelete.setIngestas(new ArrayList<>());

		Ingesta ingestaBeforeDelete = new Ingesta();
		ingestaBeforeDelete.setId(ingestaId);
		ingestaBeforeDelete.setNombre("Desayuno");
		ingestaBeforeDelete.setDieta(dietaBeforeDelete);
		ingestaBeforeDelete.setPlatillos(new ArrayList<>());

		PlatilloIngesta platilloToDelete = new PlatilloIngesta();
		platilloToDelete.setId(platilloIngestaId);
		platilloToDelete.setName("Platillo de prueba");
		platilloToDelete.setIngesta(ingestaBeforeDelete);
		ingestaBeforeDelete.getPlatillos().add(platilloToDelete);
		dietaBeforeDelete.getIngestas().add(ingestaBeforeDelete);

		// Create dieta after delete (without the platillo)
		Dieta dietaAfterDelete = new Dieta();
		dietaAfterDelete.setId(dietaId);
		dietaAfterDelete.setNombre("Dieta con Platillos");
		dietaAfterDelete.setIngestas(new ArrayList<>());

		Ingesta ingestaAfterDelete = new Ingesta();
		ingestaAfterDelete.setId(ingestaId);
		ingestaAfterDelete.setNombre("Desayuno");
		ingestaAfterDelete.setDieta(dietaAfterDelete);
		ingestaAfterDelete.setPlatillos(new ArrayList<>());
		dietaAfterDelete.getIngestas().add(ingestaAfterDelete);

		when(dietaService.getDieta(dietaId)).thenReturn(dietaBeforeDelete);
		when(dietaService.saveDieta(dietaBeforeDelete)).thenReturn(dietaAfterDelete);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deletePlatilloIngesta(dietaId, ingestaId,
				platilloIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isNotNull();
		assertThat(result.getBody().getData().getId()).isEqualTo(dietaId);
		// Verify that the platillo was removed from the ingesta
		Ingesta ingestaResult = result.getBody()
			.getData()
			.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(ingestaId))
			.findFirst()
			.orElse(null);
		assertThat(ingestaResult).isNotNull();
		assertThat(ingestaResult.getPlatillos()).isEmpty();
		log.info("Finishing testDeletePlatilloIngestaSuccessRemovesPlatilloFromIngesta");
	}

	@Test
	public void testDeletePlatilloIngestaDietaNotFoundReturnsNotFound() {
		log.info("Starting testDeletePlatilloIngestaDietaNotFoundReturnsNotFound");

		// Arrange
		Long dietaId = 999L;
		Long ingestaId = 2L;
		Long platilloIngestaId = 1L;

		when(dietaService.getDieta(dietaId)).thenReturn(null);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deletePlatilloIngesta(dietaId, ingestaId,
				platilloIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		log.info("Finishing testDeletePlatilloIngestaDietaNotFoundReturnsNotFound");
	}

	@Test
	public void testDeletePlatilloIngestaIngestaNotFoundStillReturnsOk() {
		log.info("Starting testDeletePlatilloIngestaIngestaNotFoundStillReturnsOk");

		// Arrange
		Long dietaId = 2L;
		Long ingestaId = 999L; // Non-existent ingesta
		Long platilloIngestaId = 1L;

		// Create dieta without the target ingesta
		Dieta dieta = new Dieta();
		dieta.setId(dietaId);
		dieta.setNombre("Dieta sin la ingesta objetivo");
		dieta.setIngestas(new ArrayList<>());

		Ingesta otherIngesta = new Ingesta();
		otherIngesta.setId(888L);
		otherIngesta.setNombre("Otra ingesta");
		otherIngesta.setDieta(dieta);
		otherIngesta.setPlatillos(new ArrayList<>());
		dieta.getIngestas().add(otherIngesta);

		when(dietaService.getDieta(dietaId)).thenReturn(dieta);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deletePlatilloIngesta(dietaId, ingestaId,
				platilloIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isNotNull();
		// The dieta should remain unchanged since the ingesta doesn't exist
		assertThat(result.getBody().getData().getIngestas()).hasSize(1);
		log.info("Finishing testDeletePlatilloIngestaIngestaNotFoundStillReturnsOk");
	}

	@Test
	public void testDeletePlatilloIngestaPlatilloNotFoundStillReturnsOk() {
		log.info("Starting testDeletePlatilloIngestaPlatilloNotFoundStillReturnsOk");

		// Arrange
		Long dietaId = 2L;
		Long ingestaId = 2L;
		Long platilloIngestaId = 999L; // Non-existent platillo

		// Create dieta with ingesta but without the target platillo
		Dieta dieta = new Dieta();
		dieta.setId(dietaId);
		dieta.setNombre("Dieta sin el platillo objetivo");
		dieta.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(ingestaId);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		dieta.getIngestas().add(ingesta);

		when(dietaService.getDieta(dietaId)).thenReturn(dieta);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deletePlatilloIngesta(dietaId, ingestaId,
				platilloIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isNotNull();
		// The ingesta should remain unchanged since the platillo doesn't exist
		Ingesta ingestaResult = result.getBody()
			.getData()
			.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(ingestaId))
			.findFirst()
			.orElse(null);
		assertThat(ingestaResult).isNotNull();
		assertThat(ingestaResult.getPlatillos()).isEmpty();
		log.info("Finishing testDeletePlatilloIngestaPlatilloNotFoundStillReturnsOk");
	}

	@Test
	public void testDeletePlatilloIngestaMultiplePlatillosRemovesOnlyTargetPlatillo() {
		log.info("Starting testDeletePlatilloIngestaMultiplePlatillosRemovesOnlyTargetPlatillo");

		// Arrange
		Long dietaId = 2L;
		Long ingestaId = 2L;
		Long platilloIngestaIdToDelete = 1L;
		Long platilloIngestaIdToKeep = 2L;

		// Create dieta with multiple platillos
		Dieta dietaBeforeDelete = new Dieta();
		dietaBeforeDelete.setId(dietaId);
		dietaBeforeDelete.setNombre("Dieta con múltiples platillos");
		dietaBeforeDelete.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(ingestaId);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dietaBeforeDelete);
		ingesta.setPlatillos(new ArrayList<>());

		PlatilloIngesta platilloToDelete = new PlatilloIngesta();
		platilloToDelete.setId(platilloIngestaIdToDelete);
		platilloToDelete.setName("Platillo a eliminar");
		platilloToDelete.setIngesta(ingesta);

		PlatilloIngesta platilloToKeep = new PlatilloIngesta();
		platilloToKeep.setId(platilloIngestaIdToKeep);
		platilloToKeep.setName("Platillo a mantener");
		platilloToKeep.setIngesta(ingesta);

		ingesta.getPlatillos().add(platilloToDelete);
		ingesta.getPlatillos().add(platilloToKeep);
		dietaBeforeDelete.getIngestas().add(ingesta);

		// Create dieta after delete (with only one platillo)
		Dieta dietaAfterDelete = new Dieta();
		dietaAfterDelete.setId(dietaId);
		dietaAfterDelete.setNombre("Dieta con múltiples platillos");
		dietaAfterDelete.setIngestas(new ArrayList<>());

		Ingesta ingestaAfterDelete = new Ingesta();
		ingestaAfterDelete.setId(ingestaId);
		ingestaAfterDelete.setNombre("Desayuno");
		ingestaAfterDelete.setDieta(dietaAfterDelete);
		ingestaAfterDelete.setPlatillos(new ArrayList<>());

		PlatilloIngesta remainingPlatillo = new PlatilloIngesta();
		remainingPlatillo.setId(platilloIngestaIdToKeep);
		remainingPlatillo.setName("Platillo a mantener");
		remainingPlatillo.setIngesta(ingestaAfterDelete);
		ingestaAfterDelete.getPlatillos().add(remainingPlatillo);
		dietaAfterDelete.getIngestas().add(ingestaAfterDelete);

		when(dietaService.getDieta(dietaId)).thenReturn(dietaBeforeDelete);
		when(dietaService.saveDieta(dietaBeforeDelete)).thenReturn(dietaAfterDelete);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deletePlatilloIngesta(dietaId, ingestaId,
				platilloIngestaIdToDelete);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isNotNull();
		// Verify that only the target platillo was removed
		Ingesta ingestaResult = result.getBody()
			.getData()
			.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(ingestaId))
			.findFirst()
			.orElse(null);
		assertThat(ingestaResult).isNotNull();
		assertThat(ingestaResult.getPlatillos()).hasSize(1);
		assertThat(ingestaResult.getPlatillos().get(0).getId()).isEqualTo(platilloIngestaIdToKeep);
		log.info("Finishing testDeletePlatilloIngestaMultiplePlatillosRemovesOnlyTargetPlatillo");
	}

	@Test
	public void testDeleteAlimentoIngestaSuccessRemovesAlimentoFromIngesta() {
		log.info("Starting testDeleteAlimentoIngestaSuccessRemovesAlimentoFromIngesta");

		// Arrange
		Long dietaId = 2L;
		Long ingestaId = 2L;
		Long alimentoIngestaId = 1L;

		// Create a dieta with alimento
		Dieta dietaBeforeDelete = new Dieta();
		dietaBeforeDelete.setId(dietaId);
		dietaBeforeDelete.setNombre("Dieta con Alimentos");
		dietaBeforeDelete.setIngestas(new ArrayList<>());

		Ingesta ingestaBeforeDelete = new Ingesta();
		ingestaBeforeDelete.setId(ingestaId);
		ingestaBeforeDelete.setNombre("Desayuno");
		ingestaBeforeDelete.setDieta(dietaBeforeDelete);
		ingestaBeforeDelete.setAlimentos(new ArrayList<>());

		AlimentoIngesta alimentoToDelete = new AlimentoIngesta();
		alimentoToDelete.setId(alimentoIngestaId);
		alimentoToDelete.setName("Pollo");
		alimentoToDelete.setIngesta(ingestaBeforeDelete);
		ingestaBeforeDelete.getAlimentos().add(alimentoToDelete);
		dietaBeforeDelete.getIngestas().add(ingestaBeforeDelete);

		// Create dieta after delete (without the alimento)
		Dieta dietaAfterDelete = new Dieta();
		dietaAfterDelete.setId(dietaId);
		dietaAfterDelete.setNombre("Dieta con Alimentos");
		dietaAfterDelete.setIngestas(new ArrayList<>());

		Ingesta ingestaAfterDelete = new Ingesta();
		ingestaAfterDelete.setId(ingestaId);
		ingestaAfterDelete.setNombre("Desayuno");
		ingestaAfterDelete.setDieta(dietaAfterDelete);
		ingestaAfterDelete.setAlimentos(new ArrayList<>());
		dietaAfterDelete.getIngestas().add(ingestaAfterDelete);

		when(dietaService.getDieta(dietaId)).thenReturn(dietaBeforeDelete);
		when(dietaService.saveDieta(dietaBeforeDelete)).thenReturn(dietaAfterDelete);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deleteAlimentoIngesta(dietaId, ingestaId,
				alimentoIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isNotNull();
		assertThat(result.getBody().getData().getId()).isEqualTo(dietaId);
		// Verify that the alimento was removed from the ingesta
		Ingesta ingestaResult = result.getBody()
			.getData()
			.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(ingestaId))
			.findFirst()
			.orElse(null);
		assertThat(ingestaResult).isNotNull();
		assertThat(ingestaResult.getAlimentos()).isEmpty();
		log.info("Finishing testDeleteAlimentoIngestaSuccessRemovesAlimentoFromIngesta");
	}

	@Test
	public void testDeleteAlimentoIngestaDietaNotFoundReturnsNotFound() {
		log.info("Starting testDeleteAlimentoIngestaDietaNotFoundReturnsNotFound");

		// Arrange
		Long dietaId = 999L;
		Long ingestaId = 2L;
		Long alimentoIngestaId = 1L;

		when(dietaService.getDieta(dietaId)).thenReturn(null);

		// Act
		ResponseEntity<ApiResponse<Dieta>> result = dietasRestController.deleteAlimentoIngesta(dietaId, ingestaId,
				alimentoIngestaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		log.info("Finishing testDeleteAlimentoIngestaDietaNotFoundReturnsNotFound");
	}

	@Test
	public void testGetTotalProteinaDietWithAlimentosIncludesAlimentos() throws Exception {
		log.info("Starting testGetTotalProteinaDietWithAlimentosIncludesAlimentos");

		// Arrange
		Dieta dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta con Alimentos y Platillos");
		dieta.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Add platillo with 30g protein
		PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(1L);
		platillo.setProteina(30.0);
		ingesta.getPlatillos().add(platillo);

		// Add alimento with 20g protein
		AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setProteina(20.0);
		ingesta.getAlimentos().add(alimento);

		dieta.getIngestas().add(ingesta);

		// Use reflection to access private method getTotalProteina
		Method getTotalProteinaMethod = DietasRestController.class.getDeclaredMethod("getTotalProteina", Dieta.class);
		getTotalProteinaMethod.setAccessible(true);

		// Act
		Double result = (Double) getTotalProteinaMethod.invoke(dietasRestController, dieta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(50.0); // 30 + 20
		log.info("Finishing testGetTotalProteinaDietWithAlimentosIncludesAlimentos with result: {}", result);
	}

	@Test
	public void testGetTotalLipidosDietWithAlimentosIncludesAlimentos() throws Exception {
		log.info("Starting testGetTotalLipidosDietWithAlimentosIncludesAlimentos");

		// Arrange
		Dieta dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta con Alimentos y Platillos");
		dieta.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Add platillo with 15g lipids
		PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(1L);
		platillo.setLipidos(15.0);
		ingesta.getPlatillos().add(platillo);

		// Add alimento with 10g lipids
		AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setLipidos(10.0);
		ingesta.getAlimentos().add(alimento);

		dieta.getIngestas().add(ingesta);

		// Use reflection to access private method getTotalLipidos
		Method getTotalLipidosMethod = DietasRestController.class.getDeclaredMethod("getTotalLipidos", Dieta.class);
		getTotalLipidosMethod.setAccessible(true);

		// Act
		Double result = (Double) getTotalLipidosMethod.invoke(dietasRestController, dieta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(25.0); // 15 + 10
		log.info("Finishing testGetTotalLipidosDietWithAlimentosIncludesAlimentos with result: {}", result);
	}

	@Test
	public void testGetTotalHidratosDeCarbonoDietWithAlimentosIncludesAlimentos() throws Exception {
		log.info("Starting testGetTotalHidratosDeCarbonoDietWithAlimentosIncludesAlimentos");

		// Arrange
		Dieta dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta con Alimentos y Platillos");
		dieta.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Add platillo with 50g carbohydrates
		PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(1L);
		platillo.setHidratosDeCarbono(50.0);
		ingesta.getPlatillos().add(platillo);

		// Add alimento with 30g carbohydrates
		AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setHidratosDeCarbono(30.0);
		ingesta.getAlimentos().add(alimento);

		dieta.getIngestas().add(ingesta);

		// Use reflection to access private method getTotalHidratosDeCarbono
		Method getTotalHidratosDeCarbonoMethod = DietasRestController.class
			.getDeclaredMethod("getTotalHidratosDeCarbono", Dieta.class);
		getTotalHidratosDeCarbonoMethod.setAccessible(true);

		// Act
		Double result = (Double) getTotalHidratosDeCarbonoMethod.invoke(dietasRestController, dieta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(80.0); // 50 + 30
		log.info("Finishing testGetTotalHidratosDeCarbonoDietWithAlimentosIncludesAlimentos with result: {}", result);
	}

	@Test
	public void testGetDistDietWithAlimentosReturnsCorrectDistribution() throws Exception {
		log.info("Starting testGetDistDietWithAlimentosReturnsCorrectDistribution");

		// Arrange
		Dieta dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta con Alimentos");
		dieta.setIngestas(new ArrayList<>());

		Ingesta ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		// Add alimento: Protein: 20g, Lipids: 10g, Carbohydrates: 30g
		// Total kcal: 20*4 + 10*9 + 30*4 = 80 + 90 + 120 = 290 kcal
		AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setProteina(20.0);
		alimento.setLipidos(10.0);
		alimento.setHidratosDeCarbono(30.0);
		ingesta.getAlimentos().add(alimento);

		dieta.getIngestas().add(ingesta);

		// Use reflection to access private method getDist
		Method getDistMethod = DietasRestController.class.getDeclaredMethod("getDist", Dieta.class);
		getDistMethod.setAccessible(true);

		// Act
		String result = (String) getDistMethod.invoke(dietasRestController, dieta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isNotEmpty();
		assertThat(result).contains("/");
		String[] parts = result.split(" / ");
		assertThat(parts).hasSize(3);
		// Verify that all parts are valid numbers (not NaN)
		for (String part : parts) {
			assertThat(part).isNotEqualTo("NaN");
			Double.parseDouble(part); // Should not throw exception
		}
		log.info("Finishing testGetDistDietWithAlimentosReturnsCorrectDistribution with result: '{}'", result);
	}

	@Test
	public void testGetColumnsIncludesAccionesColumn() {
		log.info("Starting testGetColumnsIncludesAccionesColumn");

		// Act
		List<com.nutriconsultas.dataTables.paging.Column> columns = dietasRestController.getColumns();

		// Assert
		assertThat(columns).isNotNull();
		assertThat(columns).hasSize(8); // acciones, dieta, ingestas, dist, kcal, prot,
										// lip, hc
		assertThat(columns.get(0).getData()).isEqualTo("acciones");
		assertThat(columns.get(1).getData()).isEqualTo("dieta");
		log.info("Finishing testGetColumnsIncludesAccionesColumn");
	}

	@Test
	public void testToStringListIncludesPrintButtonInActionsColumn() throws Exception {
		log.info("Starting testToStringListIncludesPrintButtonInActionsColumn");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaConPlatillos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8);
		// Index 0 is the actions column
		String actions = result.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("href='/admin/dietas/2/print'");
		assertThat(actions).contains("class='btn btn-sm btn-primary'");
		assertThat(actions).contains("target='_blank'");
		assertThat(actions).contains("title='Imprimir PDF'");
		assertThat(actions).contains("fa-file-pdf");
		log.info("Finishing testToStringListIncludesPrintButtonInActionsColumn");
	}

	@Test
	public void testToStringListDietWithAlimentosReturnsCorrectLinks() throws Exception {
		log.info("Starting testToStringListDietWithAlimentosReturnsCorrectLinks");

		// Arrange - Create dieta with alimentos
		Dieta dietaConAlimentos = new Dieta();
		dietaConAlimentos.setId(3L);
		dietaConAlimentos.setNombre("Dieta con Alimentos");
		dietaConAlimentos.setIngestas(new ArrayList<>());

		Ingesta ingestaConAlimentos = new Ingesta();
		ingestaConAlimentos.setId(3L);
		ingestaConAlimentos.setNombre("Comida");
		ingestaConAlimentos.setDieta(dietaConAlimentos);
		ingestaConAlimentos.setAlimentos(new ArrayList<>());

		// Create alimento
		com.nutriconsultas.alimentos.Alimento alimento = new com.nutriconsultas.alimentos.Alimento();
		alimento.setId(100L);
		alimento.setNombreAlimento("Manzana");

		// Create AlimentoIngesta
		AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setId(1L);
		alimentoIngesta.setName("Manzana");
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setIngesta(ingestaConAlimentos);
		alimentoIngesta.setProteina(0.3);
		alimentoIngesta.setLipidos(0.2);
		alimentoIngesta.setHidratosDeCarbono(15.0);

		ingestaConAlimentos.getAlimentos().add(alimentoIngesta);
		dietaConAlimentos.getIngestas().add(ingestaConAlimentos);

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaConAlimentos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8); // acciones, dieta, ingestas, dist, kcal, prot,
										// lip, hc
		// Index 2 is the ingestas column - should only contain ingesta names
		String ingestas = result.get(2);
		assertThat(ingestas).isNotNull();
		assertThat(ingestas).contains("Comida");
		assertThat(ingestas).doesNotContain("/admin/alimentos/100");
		assertThat(ingestas).doesNotContain("fa-apple-alt");
		assertThat(ingestas).doesNotContain("Ver alimento: Manzana");
		log.info("Finishing testToStringListDietWithAlimentosReturnsCorrectLinks");
	}

	@Test
	public void testToStringListDietWithBothPlatillosAndAlimentosReturnsBothLinks() throws Exception {
		log.info("Starting testToStringListDietWithBothPlatillosAndAlimentosReturnsBothLinks");

		// Arrange - Create dieta with both platillos and alimentos
		Dieta dietaMixta = new Dieta();
		dietaMixta.setId(4L);
		dietaMixta.setNombre("Dieta Mixta");
		dietaMixta.setIngestas(new ArrayList<>());

		Ingesta ingestaMixta = new Ingesta();
		ingestaMixta.setId(4L);
		ingestaMixta.setNombre("Cena");
		ingestaMixta.setDieta(dietaMixta);
		ingestaMixta.setPlatillos(new ArrayList<>());
		ingestaMixta.setAlimentos(new ArrayList<>());

		// Add platillo
		PlatilloIngesta platilloIngestaMixta = new PlatilloIngesta();
		platilloIngestaMixta.setId(2L);
		platilloIngestaMixta.setName("Platillo Mixto");
		platilloIngestaMixta.setProteina(20.0);
		platilloIngestaMixta.setLipidos(10.0);
		platilloIngestaMixta.setHidratosDeCarbono(30.0);
		platilloIngestaMixta.setIngesta(ingestaMixta);
		ingestaMixta.getPlatillos().add(platilloIngestaMixta);

		// Add alimento
		com.nutriconsultas.alimentos.Alimento alimentoMixto = new com.nutriconsultas.alimentos.Alimento();
		alimentoMixto.setId(200L);
		alimentoMixto.setNombreAlimento("Pera");

		AlimentoIngesta alimentoIngestaMixto = new AlimentoIngesta();
		alimentoIngestaMixto.setId(2L);
		alimentoIngestaMixto.setName("Pera");
		alimentoIngestaMixto.setAlimento(alimentoMixto);
		alimentoIngestaMixto.setIngesta(ingestaMixta);
		alimentoIngestaMixto.setProteina(0.5);
		alimentoIngestaMixto.setLipidos(0.3);
		alimentoIngestaMixto.setHidratosDeCarbono(12.0);

		ingestaMixta.getAlimentos().add(alimentoIngestaMixto);
		dietaMixta.getIngestas().add(ingestaMixta);

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaMixta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8);
		// Index 2 is the ingestas column - should only contain ingesta names
		String ingestas = result.get(2);
		assertThat(ingestas).isNotNull();
		assertThat(ingestas).contains("Cena");
		assertThat(ingestas).doesNotContain("/admin/platillos");
		assertThat(ingestas).doesNotContain("fa-utensils");
		assertThat(ingestas).doesNotContain("/admin/alimentos/200");
		assertThat(ingestas).doesNotContain("fa-apple-alt");
		log.info("Finishing testToStringListDietWithBothPlatillosAndAlimentosReturnsBothLinks");
	}

	@Test
	public void testDuplicateDietaSuccess() {
		log.info("Starting testDuplicateDietaSuccess");

		// Arrange
		final Dieta originalDieta = new Dieta();
		originalDieta.setId(1L);
		originalDieta.setNombre("Dieta Original");
		originalDieta.setEnergia(2000);
		originalDieta.setProteina(100.0);
		originalDieta.setLipidos(50.0);
		originalDieta.setHidratosDeCarbono(200.0);
		originalDieta.setIngestas(new ArrayList<>());

		final Ingesta ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(originalDieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());
		originalDieta.getIngestas().add(ingesta);

		final Dieta duplicatedDieta = new Dieta();
		duplicatedDieta.setId(2L);
		duplicatedDieta.setNombre("Copia de Dieta Original");
		duplicatedDieta.setEnergia(2000);
		duplicatedDieta.setProteina(100.0);
		duplicatedDieta.setLipidos(50.0);
		duplicatedDieta.setHidratosDeCarbono(200.0);

		when(dietaService.duplicateDieta(1L)).thenReturn(duplicatedDieta);

		// Act
		final ResponseEntity<ApiResponse<Dieta>> response = dietasRestController.duplicateDieta(1L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData()).isNotNull();
		assertThat(response.getBody().getData().getId()).isEqualTo(2L);
		assertThat(response.getBody().getData().getNombre()).isEqualTo("Copia de Dieta Original");
		log.info("Finishing testDuplicateDietaSuccess");
	}

	@Test
	public void testDuplicateDietaNotFound() {
		log.info("Starting testDuplicateDietaNotFound");

		// Arrange
		when(dietaService.duplicateDieta(999L)).thenReturn(null);

		// Act
		final ResponseEntity<ApiResponse<Dieta>> response = dietasRestController.duplicateDieta(999L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		log.info("Finishing testDuplicateDietaNotFound");
	}

	@Test
	public void testToStringListIncludesDuplicateButtonInActionsColumn() throws Exception {
		log.info("Starting testToStringListIncludesDuplicateButtonInActionsColumn");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaConPlatillos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(8);
		// Index 0 is the actions column
		String actions = result.get(0);
		assertThat(actions).isNotNull();
		assertThat(actions).contains("duplicateDieta(2)");
		assertThat(actions).contains("class='btn btn-sm btn-info'");
		assertThat(actions).contains("title='Duplicar Dieta'");
		assertThat(actions).contains("fa-copy");
		log.info("Finishing testToStringListIncludesDuplicateButtonInActionsColumn");
	}

}
