package com.nutriconsultas.alimentos;

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
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class AlimentoRestControllerTest {

	@InjectMocks
	private AlimentoRestController alimentoRestController;

	@Mock
	private AlimentoService alimentoService;

	private List<Alimento> allAlimentos;

	// setup the alimento service
	@BeforeEach
	public void setup() {
		log.info("setting up alimento service");
		// Read CSV file from classpath and convert to list of Alimento
		try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/alimentos.csv"))) {
			log.debug("reading alimentos from csv file");
			allAlimentos = new CsvToBeanBuilder<Alimento>(reader).withType(Alimento.class).build().parse();
			log.debug("setting up alimento service with {} alimentos", allAlimentos.size());
		}
		catch (IOException e) {
			log.error("error while reading alimentos from csv file", e);
			allAlimentos = new ArrayList<>();
		}
		log.info("finished setting up alimento service");
	}

	@Test
	public void testArray() {
		log.info("starting testArray");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, 10), pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
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
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(10);
		verify(alimentoService).findAll(any(Pageable.class));
		verify(alimentoService, times(2)).count();
		log.info("finished testArray with records {}", result.getRecordsTotal());
	}

	// Test no order given
	@Test
	public void testArrayNoOrder() {
		log.info("starting testArrayNoOrder");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, 10), pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
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
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(10);
		log.info("finished testArrayNoOrder with records {}", result.getRecordsTotal());
	}

	// Test no search given
	@Test
	public void testArrayNoSearch() {
		log.info("starting testArrayNoSearch");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, 10), pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(10);
		log.info("finished testArrayNoSearch with records {}", result.getRecordsTotal());
	}

	// Test no paging given
	@Test
	public void testArrayNoPaging() {
		log.info("starting testArrayNoPaging");
		// Arrange - When length is 0, it defaults to 10, so we'll test with length 0
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, Math.min(10, allAlimentos.size())),
				pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(0);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		// When length is 0, it defaults to 10, so we expect data
		assertThat(result.getData()).isNotEmpty();
		log.info("finished testArrayNoPaging with records {}", result.getRecordsTotal());
	}

	// Test no columns given
	@Test
	public void testArrayNoColumns() {
		log.info("starting testArrayNoColumns");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, 10), pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(10);
		log.info("finished testArrayNoColumns with records {}", result.getRecordsTotal());
	}

	// Test no data given
	@Test
	public void testArrayNoData() {
		log.info("starting testArrayNoData");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn(0L);

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
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
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(0);
		assertThat(result.getRecordsFiltered()).isEqualTo(0);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isEmpty();
		log.info("finished testArrayNoData with records {}", result.getRecordsTotal());
	}

	// Test no draw given
	@Test
	public void testArrayNoDraw() {
		log.info("starting testArrayNoDraw");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(allAlimentos.subList(0, 10), pageable, allAlimentos.size());
		when(alimentoService.findAll(any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(0);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(10);
		log.info("finished testArrayNoDraw with records {}", result.getRecordsTotal());
	}

	// Test no records filtered given
	@Test
	public void testArrayNoRecordsFiltered() {
		log.info("starting testArrayNoRecordsFiltered");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
		when(alimentoService.findBySearchTerm(eq("not a valid record"), any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.countBySearchTerm(eq("not a valid record"))).thenReturn(0L);
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("not a valid record", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(0);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isEmpty();
		verify(alimentoService).findBySearchTerm(eq("not a valid record"), any(Pageable.class));
		verify(alimentoService).countBySearchTerm(eq("not a valid record"));
		verify(alimentoService).count();
		log.info("finished testArrayNoRecordsFiltered with records {}", result.getRecordsTotal());
	}

	// Test filtering records
	@Test
	public void testArrayFiltering() {
		log.info("starting testArrayFiltering");
		// Arrange - Find alimentos matching "mango"
		final List<Alimento> filteredAlimentos = allAlimentos.stream()
			.filter(a -> a.getNombreAlimento().toLowerCase().contains("mango")
					|| a.getClasificacion().toLowerCase().contains("mango"))
			.toList();
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Alimento> springPage = new PageImpl<>(filteredAlimentos, pageable, filteredAlimentos.size());
		when(alimentoService.findBySearchTerm(eq("mango"), any(Pageable.class))).thenReturn(springPage);
		when(alimentoService.countBySearchTerm(eq("mango"))).thenReturn((long) filteredAlimentos.size());
		when(alimentoService.count()).thenReturn((long) allAlimentos.size());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("alimento", "", true, true, new Search("leche", "false")));
		columnList.add(new Column("grupo", "", true, true, new Search("", "false")));
		columnList.add(new Column("cantidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("unidad", "", true, true, new Search("", "false")));
		columnList.add(new Column("bruto", "", true, true, new Search("", "false")));
		columnList.add(new Column("neto", "", true, true, new Search("", "false")));
		columnList.add(new Column("kcal", "", true, true, new Search("", "false")));
		columnList.add(new Column("prot", "", true, true, new Search("", "false")));
		columnList.add(new Column("lip", "", true, true, new Search("", "false")));
		columnList.add(new Column("hc", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("mango", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = alimentoRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allAlimentos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(filteredAlimentos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(filteredAlimentos.size());
		verify(alimentoService).findBySearchTerm(eq("mango"), any(Pageable.class));
		verify(alimentoService).countBySearchTerm(eq("mango"));
		verify(alimentoService).count();
		log.info("finished testArrayFiltering with records {}", result.getRecordsTotal());
	}

}
