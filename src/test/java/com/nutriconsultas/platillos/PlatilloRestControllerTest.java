package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
		when(platilloAuthorization.resolveCreateUserId(any(), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));
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
		verify(platilloAuthorization).resolveCreateUserId(principal, TEST_USER_ID);
	}

	@Test
	public void testAddIgnoresClientSuppliedSystemUserId() {
		final Platillo newPlatillo = new Platillo();
		newPlatillo.setName("Spoofed platillo");
		newPlatillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);

		when(platilloAuthorization.resolveCreateUserId(any(), eq(TEST_USER_ID))).thenReturn(TEST_USER_ID);
		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo saved = invocation.getArgument(0);
			saved.setId(43L);
			return saved;
		});

		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final Platillo result = platilloRestController.add(newPlatillo, principal);

		assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
		verify(platilloAuthorization).resolveCreateUserId(principal, TEST_USER_ID);
	}

	@Test
	public void testAddAsPlatformAdminSetsSystemUserId() {
		final String adminUserId = "auth0|platform-admin-test";
		final OidcIdToken adminToken = OidcIdToken.withTokenValue("admin-token")
			.subject(adminUserId)
			.claim("name", "Platform Admin")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(3600))
			.build();
		final DefaultOidcUser adminPrincipal = new DefaultOidcUser(Collections.emptyList(), adminToken);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(adminPrincipal, null));

		final Platillo newPlatillo = new Platillo();
		newPlatillo.setName("System platillo");

		when(platilloAuthorization.resolveCreateUserId(adminPrincipal, adminUserId))
			.thenReturn(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo saved = invocation.getArgument(0);
			assertThat(saved.getUserId()).isEqualTo(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
			saved.setId(99L);
			return saved;
		});

		final Platillo result = platilloRestController.add(newPlatillo, adminPrincipal);

		assertThat(result.getUserId()).isEqualTo(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		verify(platilloAuthorization).auditSystemPlatilloMutationIfNeeded(adminPrincipal, result, "platillos.create");
	}

	@Test
	public void testArray() {
		log.info("starting testArray");
		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(allPlatillos);

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
		columnList.add(new Column("imagen", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(2, Direction.asc)));
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
		columnList.add(new Column("imagen", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(2, Direction.asc)));
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
		columnList.add(new Column("imagen", "", true, true, new Search("", "false")));
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

	@Test
	public void testDuplicatePlatilloSuccess() {
		final Platillo original = new Platillo();
		original.setId(97L);
		original.setName("Frijoles con tortilla");
		original.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);

		final Platillo duplicated = new Platillo();
		duplicated.setId(100L);
		duplicated.setName("Frijoles con tortilla (copia)");
		duplicated.setUserId(TEST_USER_ID);

		when(platilloService.findById(97L)).thenReturn(original);
		when(platilloAuthorization.canCopy(original, TEST_USER_ID)).thenReturn(true);
		when(platilloService.duplicatePlatillo(97L, TEST_USER_ID)).thenReturn(duplicated);

		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final ResponseEntity<ApiResponse<Platillo>> response = platilloRestController.duplicatePlatillo(97L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData()).isNotNull();
		assertThat(response.getBody().getData().getId()).isEqualTo(100L);
		assertThat(response.getBody().getData().getUserId()).isEqualTo(TEST_USER_ID);
		verify(platilloService).duplicatePlatillo(97L, TEST_USER_ID);
	}

	@Test
	public void testDuplicatePlatilloNotFound() {
		when(platilloService.findById(999L)).thenReturn(null);

		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final ResponseEntity<ApiResponse<Platillo>> response = platilloRestController.duplicatePlatillo(999L,
				principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(platilloService, never()).duplicatePlatillo(org.mockito.ArgumentMatchers.anyLong(), anyString());
	}

	@Test
	public void testDuplicatePlatilloForbiddenForOtherOwner() {
		final Platillo original = new Platillo();
		original.setId(5L);
		original.setUserId("auth0|other-nutritionist");

		when(platilloService.findById(5L)).thenReturn(original);
		when(platilloAuthorization.canCopy(original, TEST_USER_ID)).thenReturn(false);

		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final ResponseEntity<ApiResponse<Platillo>> response = platilloRestController.duplicatePlatillo(5L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		verify(platilloService, never()).duplicatePlatillo(org.mockito.ArgumentMatchers.anyLong(), anyString());
	}

	@Test
	public void testArrayWithSinImagenFilter() {
		final Platillo withImage = new Platillo();
		withImage.setId(10L);
		withImage.setName("Con imagen");
		withImage.setImageUrl("platillo/10/picture.jpg");
		withImage.setEnergia(100);
		withImage.setProteina(1.0);
		withImage.setLipidos(1.0);
		withImage.setHidratosDeCarbono(1.0);

		final Platillo withoutImage = new Platillo();
		withoutImage.setId(11L);
		withoutImage.setName("Sin imagen");
		withoutImage.setEnergia(100);
		withoutImage.setProteina(1.0);
		withoutImage.setLipidos(1.0);
		withoutImage.setHidratosDeCarbono(1.0);

		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(List.of(withImage, withoutImage));

		final PagingRequest pagingRequest = buildPagingRequest("sin-imagen");

		final PageArray result = platilloRestController.getPageArray(pagingRequest);

		assertThat(result.getRecordsTotal()).isEqualTo(1);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getData().get(0).get(2)).contains("Sin imagen");
		assertThat(result.getData().get(0).get(1)).contains("Sin imagen");
	}

	@Test
	public void testToStringListIncludesCopyButtonForSystemPlatillo() {
		final Platillo systemPlatillo = new Platillo();
		systemPlatillo.setId(97L);
		systemPlatillo.setName("Frijoles con tortilla");
		systemPlatillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		systemPlatillo.setEnergia(200);
		systemPlatillo.setProteina(10.0);
		systemPlatillo.setLipidos(5.0);
		systemPlatillo.setHidratosDeCarbono(30.0);

		when(platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS, TEST_USER_ID))
			.thenReturn(List.of(systemPlatillo));
		when(platilloAuthorization.canCopy(systemPlatillo, TEST_USER_ID)).thenReturn(true);
		when(platilloAuthorization.canModify(systemPlatillo, TEST_USER_ID,
				(OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
			.thenReturn(false);

		final PagingRequest pagingRequest = new PagingRequest();
		final List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
		columnList.add(new Column("imagen", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(2, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setOwnershipFilter("todas");

		final PageArray result = platilloRestController.getPageArray(pagingRequest);
		final String actions = result.getData().get(0).get(0);

		assertThat(actions).contains("duplicatePlatillo(97)");
		assertThat(actions).doesNotContain("fa-edit");
		assertThat(actions).doesNotContain("deletePlatillo");
	}

	private PagingRequest buildPagingRequest(final String pictureFilter) {
		final PagingRequest pagingRequest = new PagingRequest();
		final List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("acciones", "", true, true, new Search("", "false")));
		columnList.add(new Column("imagen", "", true, true, new Search("", "false")));
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
		pagingRequest.setOrder(Arrays.asList(new Order(2, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setOwnershipFilter("todas");
		pagingRequest.setPictureFilter(pictureFilter);
		return pagingRequest;
	}

}
