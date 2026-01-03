package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.model.ApiResponse;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PlatilloRestControllerTest {

	@InjectMocks
	private PlatilloRestController platilloRestController;

	@Mock
	private PlatilloService platilloService;

	private List<Platillo> allPlatillos;

	@BeforeEach
	public void setup() {
		log.info("setting up platillo service");
		// Read CSV file from classpath and convert to list of Platillo
		try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/platillos.csv"))) {
			log.debug("reading platillos from csv file");
			allPlatillos = new CsvToBeanBuilder<Platillo>(reader).withType(Platillo.class)
				.withIgnoreLeadingWhiteSpace(true)
				.build()
				.parse();
			log.debug("setting up platillo service with {} platillos", allPlatillos.size());
		}
		catch (IOException e) {
			log.error("error while reading platillos from csv file", e);
			allPlatillos = new ArrayList<>();
		}
		log.info("finished setting up platillo service");
	}

	@Test
	public void testAdd() {
		log.info("Starting testAdd");
		// Arrange
		Platillo platillo = new Platillo();
		platillo.setName("Test platillo");
		log.debug("Platillo to add: {}", platillo);

		// Mock the save method
		when(platilloService.save(any(Platillo.class))).thenReturn(platillo);

		// Act
		Platillo result = platilloRestController.add(platillo);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(platillo.getName());
	}

	@Test
	public void testArray() {
		log.info("starting testArray");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Platillo> springPage = new PageImpl<>(allPlatillos.subList(0, Math.min(10, allPlatillos.size())),
				pageable, allPlatillos.size());
		when(platilloService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(platilloService.count()).thenReturn((long) allPlatillos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("platillo", "", true, true, new Search("", "false")));
		columnList.add(new Column("ingestas", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));

		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = platilloRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allPlatillos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allPlatillos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(Math.min(10, allPlatillos.size()));
		verify(platilloService).findAll(any(Pageable.class));
		verify(platilloService, times(2)).count();
		log.info("finished testArray with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayWithSearch() {
		log.info("starting testArrayWithSearch");
		// Arrange - Find platillos matching search term
		final String searchTerm = "test";
		final List<Platillo> filteredPlatillos = allPlatillos.stream()
			.filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(searchTerm))
					|| (p.getIngestasSugeridas() != null
							&& p.getIngestasSugeridas().toLowerCase().contains(searchTerm)))
			.toList();
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Platillo> springPage = new PageImpl<>(filteredPlatillos, pageable, filteredPlatillos.size());
		when(platilloService.findBySearchTerm(eq(searchTerm), any(Pageable.class))).thenReturn(springPage);
		when(platilloService.countBySearchTerm(eq(searchTerm))).thenReturn((long) filteredPlatillos.size());
		when(platilloService.count()).thenReturn((long) allPlatillos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("platillo", "", true, true, new Search("", "false")));
		columnList.add(new Column("ingestas", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search(searchTerm, "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = platilloRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allPlatillos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(filteredPlatillos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		verify(platilloService).findBySearchTerm(eq(searchTerm), any(Pageable.class));
		verify(platilloService).countBySearchTerm(eq(searchTerm));
		verify(platilloService).count();
		log.info("finished testArrayWithSearch with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayNoOrder() {
		log.info("starting testArrayNoOrder");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Platillo> springPage = new PageImpl<>(allPlatillos.subList(0, Math.min(10, allPlatillos.size())),
				pageable, allPlatillos.size());
		when(platilloService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(platilloService.count()).thenReturn((long) allPlatillos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("platillo", "", true, true, new Search("", "false")));
		columnList.add(new Column("ingestas", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = platilloRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allPlatillos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allPlatillos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("finished testArrayNoOrder with records {}", result.getRecordsTotal());
	}

	// Test addIngrediente
	@Test
	public void testAddIngrediente() {
		log.info("Starting testAddIngrediente");
		// Arrange
		Platillo platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Test platillo");
		log.debug("Platillo to add ingrediente: {}", platillo);

		Alimento alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Test alimento");
		alimento.setCantSugerida(1d);
		alimento.setUnidad("pieza");
		alimento.setPesoNeto(100);
		alimento.setPesoBrutoRedondeado(100);

		IngredienteFormModel ingrediente = new IngredienteFormModel();
		ingrediente.setAlimentoId(1L);
		ingrediente.setCantidad("1");
		ingrediente.setPeso(100);

		Ingrediente ingredienteResult = new Ingrediente();
		ingredienteResult.setId(1L);
		ingredienteResult.setAlimento(alimento);
		ingredienteResult.setCantSugerida(1d);
		ingredienteResult.setPesoNeto(100);

		log.debug("Ingrediente to add: {}", ingrediente);

		// Mock the save method
		when(platilloService.addIngrediente(1L, 1L, "1", 100)).thenReturn(ingredienteResult);

		// Act
		ResponseEntity<ApiResponse<Ingrediente>> result = platilloRestController.addIngrediente(1L, ingrediente);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(result.getBody().getData()).isEqualTo(ingredienteResult);
	}

}
