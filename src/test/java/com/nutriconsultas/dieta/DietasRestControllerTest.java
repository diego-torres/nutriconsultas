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
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;

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
	public void testGetDist_EmptyDiet_ReturnsEmptyString() throws Exception {
		log.info("Starting testGetDist_EmptyDiet_ReturnsEmptyString");

		// Use reflection to access private method getDist
		Method getDistMethod = DietasRestController.class.getDeclaredMethod("getDist", Dieta.class);
		getDistMethod.setAccessible(true);

		// Act
		String result = (String) getDistMethod.invoke(dietasRestController, dietaVacia);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		log.info("Finishing testGetDist_EmptyDiet_ReturnsEmptyString with result: '{}'", result);
	}

	@Test
	public void testGetDist_DietWithPlatillos_ReturnsCorrectDistribution() throws Exception {
		log.info("Starting testGetDist_DietWithPlatillos_ReturnsCorrectDistribution");

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
		log.info("Finishing testGetDist_DietWithPlatillos_ReturnsCorrectDistribution with result: '{}'", result);
	}

	@Test
	public void testToStringList_EmptyDiet_ReturnsEmptyStringForDistribution() throws Exception {
		log.info("Starting testToStringList_EmptyDiet_ReturnsEmptyStringForDistribution");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaVacia);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(7); // dieta, ingestas, dist, kcal, prot, lip, hc
		// Index 2 is the distribution column
		String distribution = result.get(2);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isEmpty();
		log.info("Finishing testToStringList_EmptyDiet_ReturnsEmptyStringForDistribution with distribution: '{}'",
				distribution);
	}

	@Test
	public void testToStringList_DietWithPlatillos_ReturnsCorrectValues() throws Exception {
		log.info("Starting testToStringList_DietWithPlatillos_ReturnsCorrectValues");

		// Use reflection to access protected method toStringList
		Method toStringListMethod = DietasRestController.class.getDeclaredMethod("toStringList", Dieta.class);
		toStringListMethod.setAccessible(true);

		// Act
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) toStringListMethod.invoke(dietasRestController, dietaConPlatillos);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(7); // dieta, ingestas, dist, kcal, prot, lip, hc
		// Index 2 is the distribution column
		String distribution = result.get(2);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isNotEmpty();
		assertThat(distribution).doesNotContain("NaN");
		// Index 3 is kcal
		String kcal = result.get(3);
		assertThat(kcal).isEqualTo("455.0");
		// Index 4 is protein
		String protein = result.get(4);
		assertThat(protein).isEqualTo("30.0");
		// Index 5 is lipids
		String lipids = result.get(5);
		assertThat(lipids).isEqualTo("15.0");
		// Index 6 is carbohydrates
		String carbohydrates = result.get(6);
		assertThat(carbohydrates).isEqualTo("50.0");
		log.info("Finishing testToStringList_DietWithPlatillos_ReturnsCorrectValues");
	}

	@Test
	public void testGetPageArray_EmptyDiet_ShowsEmptyDistribution() {
		log.info("Starting testGetPageArray_EmptyDiet_ShowsEmptyDistribution");

		// Arrange
		List<Dieta> dietas = Arrays.asList(dietaVacia);
		when(dietaService.getDietas()).thenReturn(dietas);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = dietasRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).isNotEmpty();
		List<String> firstRow = (List<String>) result.getData().get(0);
		String distribution = firstRow.get(2);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isEmpty();
		assertThat(distribution).doesNotContain("NaN");
		log.info("Finishing testGetPageArray_EmptyDiet_ShowsEmptyDistribution");
	}

	@Test
	public void testGetPageArray_DietWithPlatillos_ShowsCorrectDistribution() {
		log.info("Starting testGetPageArray_DietWithPlatillos_ShowsCorrectDistribution");

		// Arrange
		List<Dieta> dietas = Arrays.asList(dietaConPlatillos);
		when(dietaService.getDietas()).thenReturn(dietas);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = dietasRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).isNotEmpty();
		List<String> firstRow = (List<String>) result.getData().get(0);
		String distribution = firstRow.get(2);
		assertThat(distribution).isNotNull();
		assertThat(distribution).isNotEmpty();
		assertThat(distribution).doesNotContain("NaN");
		log.info("Finishing testGetPageArray_DietWithPlatillos_ShowsCorrectDistribution");
	}

}
