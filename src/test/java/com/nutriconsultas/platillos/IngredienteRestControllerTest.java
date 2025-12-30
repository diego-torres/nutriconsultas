package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class IngredienteRestControllerTest {

	@InjectMocks
	private IngredienteRestController ingredienteRestController;

	@Mock
	private PlatilloService platilloService;

	private Platillo platillo;

	private Ingrediente ingrediente;

	private Alimento alimento;

	@BeforeEach
	public void setup() {
		log.info("setting up IngredienteRestController test");

		alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Test Alimento");

		ingrediente = new Ingrediente();
		ingrediente.setId(1L);
		ingrediente.setAlimento(alimento);
		ingrediente.setCantSugerida(0.5);
		ingrediente.setUnidad("pieza");
		ingrediente.setPesoNeto(100);

		platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Test Platillo");
		platillo.setIngredientes(Arrays.asList(ingrediente));

		log.info("finished setting up IngredienteRestController test");
	}

	@Test
	public void testDelete() {
		log.info("Starting testDelete");
		// Act
		ingredienteRestController.delete(1L, 1L);

		// Assert
		verify(platilloService).deleteIngrediente(1L, 1L);
		log.info("Finishing testDelete");
	}

	@Test
	public void testToStringList() {
		log.info("Starting testToStringList");
		// Act
		List<String> result = ingredienteRestController.toStringList(ingrediente);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.get(0)).isEqualTo("Test Alimento");
		assertThat(result.get(1)).isEqualTo("1/2");
		assertThat(result.get(2)).isEqualTo("pieza");
		assertThat(result.get(3)).isEqualTo("100");
		assertThat(result.get(4)).contains("delete-btn");
		log.info("Finishing testToStringList");
	}

	@Test
	public void testGetColumns() {
		log.info("Starting testGetColumns");
		// Act
		List<Column> result = ingredienteRestController.getColumns();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.get(0).getData()).isEqualTo("ingrediente");
		assertThat(result.get(1).getData()).isEqualTo("cantidad");
		assertThat(result.get(2).getData()).isEqualTo("unidad");
		assertThat(result.get(3).getData()).isEqualTo("peso");
		assertThat(result.get(4).getData()).isEqualTo("acciones");
		log.info("Finishing testGetColumns");
	}

	@Test
	public void testGetData() {
		log.info("Starting testGetData");
		// Arrange
		when(platilloService.findById(1L)).thenReturn(platillo);

		// Act
		List<Ingrediente> result = ingredienteRestController.getData(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0)).isEqualTo(ingrediente);
		verify(platilloService).findById(1L);
		log.info("Finishing testGetData");
	}

	@Test
	public void testGetPredicate() {
		log.info("Starting testGetPredicate");
		// Act
		var predicate = ingredienteRestController.getPredicate("test");

		// Assert
		assertThat(predicate).isNotNull();
		assertThat(predicate.test(ingrediente)).isTrue();

		// Test with non-matching value
		var predicate2 = ingredienteRestController.getPredicate("xyz");
		assertThat(predicate2.test(ingrediente)).isFalse();
		log.info("Finishing testGetPredicate");
	}

	@Test
	public void testGetComparator() {
		log.info("Starting testGetComparator");
		// Act
		var comparator = ingredienteRestController.getComparator("ingrediente", Direction.asc);

		// Assert
		assertThat(comparator).isNotNull();

		// Test sorting
		Ingrediente ingrediente2 = new Ingrediente();
		Alimento alimento2 = new Alimento();
		alimento2.setNombreAlimento("Another Alimento");
		ingrediente2.setAlimento(alimento2);

		int result = comparator.compare(ingrediente, ingrediente2);
		assertThat(result).isGreaterThan(0); // "Test Alimento" > "Another Alimento"

		// Test descending
		var descComparator = ingredienteRestController.getComparator("ingrediente", Direction.desc);
		int descResult = descComparator.compare(ingrediente, ingrediente2);
		assertThat(descResult).isLessThan(0);
		log.info("Finishing testGetComparator");
	}

	@Test
	public void testGetPageArray() {
		log.info("Starting testGetPageArray");
		// Arrange
		when(platilloService.findById(1L)).thenReturn(platillo);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = ingredienteRestController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(1);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("Finishing testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("Starting testGetPageArrayWithSearch");
		// Arrange
		when(platilloService.findById(1L)).thenReturn(platillo);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("Test", "false"));

		// Act
		PageArray result = ingredienteRestController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(1);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		log.info("Finishing testGetPageArrayWithSearch");
	}

	@Test
	public void testGetPageArrayWithNoMatch() {
		log.info("Starting testGetPageArrayWithNoMatch");
		// Arrange
		when(platilloService.findById(1L)).thenReturn(platillo);

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("NonExistent", "false"));
		pagingRequest.setColumns(ingredienteRestController.getColumns());

		// Act
		PageArray result = ingredienteRestController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(1);
		assertThat(result.getRecordsFiltered()).isEqualTo(0);
		assertThat(result.getData()).isEmpty();
		log.info("Finishing testGetPageArrayWithNoMatch");
	}

}
