package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;

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
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PlatilloRestControllerTest {

	@InjectMocks
	private PlatilloRestController platilloRestController;

	@Mock
	private PlatilloService platilloService;

	@Mock
	private PlatilloAuthorization platilloAuthorization;

	@Mock
	private PlatilloDeletionService platilloDeletionService;

	private List<Platillo> allPlatillos;

	private Platillo platillo;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up platillo service");
		platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Test platillo");

		final OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
			.subject(TEST_USER_ID)
			.claim("name", "Test User")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(3600))
			.build();
		final DefaultOidcUser oidcUser = new DefaultOidcUser(Collections.emptyList(), idToken);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(oidcUser, null));

		when(platilloAuthorization.resolveForMutation(any(), any(), any(), eq(platilloService)))
			.thenAnswer(invocation -> platilloService.findById(invocation.getArgument(0)));
		doNothing().when(platilloAuthorization).verifyCanModify(any(), any(), any());
		doNothing().when(platilloAuthorization).auditSystemPlatilloMutationIfNeeded(any(), any(), anyString());
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

	@AfterEach
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testAdd() {
		log.info("Starting testAdd");
		final Platillo newPlatillo = new Platillo();
		newPlatillo.setName("Test platillo");
		log.debug("Platillo to add: {}", newPlatillo);

		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo saved = invocation.getArgument(0);
			saved.setId(42L);
			return saved;
		});

		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final Platillo result = platilloRestController.add(newPlatillo, principal);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(newPlatillo.getName());
		assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
	}

	@Test
	public void testArray() {
		log.info("starting testArray");
		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(allPlatillos);

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(1, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setOwnershipFilter("todas");
		log.debug("arrange paging request {}.", pagingRequest);

		PageArray result = platilloRestController.getPageArray(pagingRequest);

		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allPlatillos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(allPlatillos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(Math.min(10, allPlatillos.size()));
		verify(platilloService).getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID);
		log.info("finished testArray with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayWithSearch() {
		log.info("starting testArrayWithSearch");
		final String searchTerm = "test";
		final List<Platillo> filteredPlatillos = allPlatillos.stream()
			.filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(searchTerm))
					|| (p.getIngestasSugeridas() != null
							&& p.getIngestasSugeridas().toLowerCase().contains(searchTerm)))
			.toList();
		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(allPlatillos);

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(1, Direction.asc)));
		pagingRequest.setSearch(new Search(searchTerm, "false"));
		pagingRequest.setOwnershipFilter("todas");
		log.debug("arrange paging request {}.", pagingRequest);

		PageArray result = platilloRestController.getPageArray(pagingRequest);

		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(allPlatillos.size());
		assertThat(result.getRecordsFiltered()).isEqualTo(filteredPlatillos.size());
		assertThat(result.getDraw()).isEqualTo(1);
		verify(platilloService).getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID);
		log.info("finished testArrayWithSearch with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayNoOrder() {
		log.info("starting testArrayNoOrder");
		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(allPlatillos);

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
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
		pagingRequest.setOwnershipFilter("todas");

		PageArray result = platilloRestController.getPageArray(pagingRequest);

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

		when(platilloService.findById(1L)).thenReturn(platillo);
		when(platilloService.addIngrediente(1L, 1L, "1", 100)).thenReturn(ingredienteResult);

		// Act
		ResponseEntity<ApiResponse<Ingrediente>> result = platilloRestController.addIngrediente(1L, ingrediente);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(result.getBody().getData()).isEqualTo(ingredienteResult);
	}

	@Test
	public void testAddIngredienteForbiddenOnSystemCatalog() {
		final Platillo systemPlatillo = new Platillo();
		systemPlatillo.setId(97L);
		systemPlatillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		when(platilloService.findById(97L)).thenReturn(systemPlatillo);
		when(platilloAuthorization.resolveForMutation(eq(97L), eq(TEST_USER_ID), any(), eq(platilloService)))
			.thenReturn(null);

		final IngredienteFormModel ingrediente = new IngredienteFormModel();
		ingrediente.setAlimentoId(1L);
		ingrediente.setCantidad("1");
		ingrediente.setPeso(100);

		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> platilloRestController.addIngrediente(97L, ingrediente))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

}
