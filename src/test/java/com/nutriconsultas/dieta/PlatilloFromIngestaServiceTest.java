package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloAuthorization;
import com.nutriconsultas.platillos.PlatilloService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlatilloFromIngestaServiceTest {

	private static final String TEST_USER_ID = "auth0|nutritionist-one";

	@InjectMocks
	private PlatilloFromIngestaServiceImpl service;

	@Mock
	private PlatilloService platilloService;

	@Mock
	private PlatilloAuthorization platilloAuthorization;

	@Mock
	private DietaService dietaService;

	@Mock
	private OidcUser principal;

	private Ingesta ingesta;

	private Alimento arroz;

	private Alimento frijol;

	@BeforeEach
	void setup() {
		ingesta = new Ingesta("Comida");
		ingesta.setId(10L);
		ingesta.setAlimentos(new ArrayList<>());
		ingesta.setPlatillos(new ArrayList<>());

		arroz = catalogAlimento(100L, "Arroz", 1.0, 150, "porción");
		frijol = catalogAlimento(101L, "Frijol", 0.5, 80, "porción");
	}

	@Test
	void createFromIngestaSelection_createsPlatilloFromTwoAlimentos() {
		final AlimentoIngesta arrozIngesta = alimentoIngesta(1L, arroz, 2);
		final AlimentoIngesta frijolIngesta = alimentoIngesta(2L, frijol, 1);
		ingesta.getAlimentos().addAll(List.of(arrozIngesta, frijolIngesta));

		when(platilloAuthorization.resolveCreateUserId(principal, TEST_USER_ID)).thenReturn(TEST_USER_ID);
		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo platillo = invocation.getArgument(0);
			platillo.setId(500L);
			return platillo;
		});
		when(platilloService.addIngrediente(eq(500L), eq(100L), any(), any())).thenReturn(new Ingrediente());
		when(platilloService.addIngrediente(eq(500L), eq(101L), any(), any())).thenReturn(new Ingrediente());
		when(platilloService.findById(500L)).thenReturn(savedPlatillo(500L, "Ensalada mixta"));

		final CreatePlatilloFromIngestaRequest request = new CreatePlatilloFromIngestaRequest();
		request.setNombre("Ensalada mixta");
		request.setAlimentoIngestaIds(List.of(1L, 2L));

		final Platillo result = service.createFromIngestaSelection(ingesta, request, TEST_USER_ID, principal);

		assertThat(result.getId()).isEqualTo(500L);
		assertThat(result.getName()).isEqualTo("Ensalada mixta");
		verify(platilloService).addIngrediente(eq(500L), eq(100L), eq("2"), eq(300));
		verify(platilloService).addIngrediente(eq(500L), eq(101L), eq("1/2"), eq(80));
	}

	@Test
	void createFromIngestaSelection_rejectsBlankName() {
		final CreatePlatilloFromIngestaRequest request = new CreatePlatilloFromIngestaRequest();
		request.setNombre("   ");
		request.setAlimentoIngestaIds(List.of(1L));

		assertThatThrownBy(() -> service.createFromIngestaSelection(ingesta, request, TEST_USER_ID, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("nombre");
	}

	@Test
	void createFromIngestaSelection_rejectsSinglePlatilloOnly() {
		final PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(20L);
		ingesta.getPlatillos().add(platilloIngesta);

		final CreatePlatilloFromIngestaRequest request = new CreatePlatilloFromIngestaRequest();
		request.setNombre("Copia");
		request.setPlatilloIngestaIds(List.of(20L));

		assertThatThrownBy(() -> service.createFromIngestaSelection(ingesta, request, TEST_USER_ID, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("catálogo");
	}

	@Test
	void createFromIngestaSelection_mergesPlatilloIngredientsWithAlimentos() {
		final AlimentoIngesta arrozIngesta = alimentoIngesta(1L, arroz, 1);
		ingesta.getAlimentos().add(arrozIngesta);

		final IngredientePlatilloIngesta platilloFrijol = new IngredientePlatilloIngesta();
		platilloFrijol.setAlimento(frijol);
		platilloFrijol.setCantSugerida(0.5);
		platilloFrijol.setPesoNeto(80);
		platilloFrijol.setPesoBrutoRedondeado(80);
		platilloFrijol.setUnidad("porción");

		final PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(20L);
		platilloIngesta.setPortions(2);
		platilloIngesta.setIngredientes(new ArrayList<>(List.of(platilloFrijol)));
		ingesta.getPlatillos().add(platilloIngesta);

		when(platilloAuthorization.resolveCreateUserId(principal, TEST_USER_ID)).thenReturn(TEST_USER_ID);
		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo platillo = invocation.getArgument(0);
			platillo.setId(501L);
			return platillo;
		});
		when(platilloService.addIngrediente(eq(501L), eq(100L), any(), any())).thenReturn(new Ingrediente());
		when(platilloService.addIngrediente(eq(501L), eq(101L), any(), any())).thenReturn(new Ingrediente());
		when(platilloService.findById(501L)).thenReturn(savedPlatillo(501L, "Combo"));

		final CreatePlatilloFromIngestaRequest request = new CreatePlatilloFromIngestaRequest();
		request.setNombre("Combo");
		request.setAlimentoIngestaIds(List.of(1L));
		request.setPlatilloIngestaIds(List.of(20L));

		service.createFromIngestaSelection(ingesta, request, TEST_USER_ID, principal);

		verify(platilloService).addIngrediente(eq(501L), eq(100L), eq("1"), eq(150));
		verify(platilloService).addIngrediente(eq(501L), eq(101L), eq("1"), eq(160));
	}

	@Test
	void createFromIngestaSelection_setsIngestasSugeridasFromIngestaName() {
		final AlimentoIngesta arrozIngesta = alimentoIngesta(1L, arroz, 1);
		ingesta.getAlimentos().add(arrozIngesta);

		when(platilloAuthorization.resolveCreateUserId(principal, TEST_USER_ID)).thenReturn(TEST_USER_ID);
		when(platilloService.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo platillo = invocation.getArgument(0);
			platillo.setId(502L);
			assertThat(platillo.getIngestasSugeridas()).isEqualTo("Comida");
			return platillo;
		});
		when(platilloService.addIngrediente(any(), any(), any(), any())).thenReturn(new Ingrediente());
		when(platilloService.findById(502L)).thenReturn(savedPlatillo(502L, "Arroz solo"));

		final CreatePlatilloFromIngestaRequest request = new CreatePlatilloFromIngestaRequest();
		request.setNombre("Arroz solo");
		request.setAlimentoIngestaIds(List.of(1L));

		service.createFromIngestaSelection(ingesta, request, TEST_USER_ID, principal);

		final ArgumentCaptor<Platillo> captor = ArgumentCaptor.forClass(Platillo.class);
		verify(platilloService).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(TEST_USER_ID);
	}

	@Test
	void replaceSelectionWithCatalogPlatillo_removesSelectedRowsAndAddsPlatilloIngesta() {
		final AlimentoIngesta arrozIngesta = alimentoIngesta(1L, arroz, 1);
		final AlimentoIngesta frijolIngesta = alimentoIngesta(2L, frijol, 1);
		ingesta.getAlimentos().addAll(List.of(arrozIngesta, frijolIngesta));

		final Dieta dieta = new Dieta();
		dieta.setId(4L);
		dieta.setIngestas(new ArrayList<>(List.of(ingesta)));

		final Ingrediente catalogArroz = new Ingrediente();
		catalogArroz.setAlimento(arroz);
		catalogArroz.setCantSugerida(1.0);
		catalogArroz.setPesoNeto(150);

		final Ingrediente catalogFrijol = new Ingrediente();
		catalogFrijol.setAlimento(frijol);
		catalogFrijol.setCantSugerida(0.5);
		catalogFrijol.setPesoNeto(80);

		final Platillo catalogPlatillo = new Platillo();
		catalogPlatillo.setId(200L);
		catalogPlatillo.setName("Combo");
		catalogPlatillo.setIngredientes(new ArrayList<>(List.of(catalogArroz, catalogFrijol)));

		when(platilloService.findById(200L)).thenReturn(catalogPlatillo);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		final ReplaceIngestaSelectionRequest request = new ReplaceIngestaSelectionRequest();
		request.setAlimentoIngestaIds(List.of(1L, 2L));

		final Dieta result = service.replaceSelectionWithCatalogPlatillo(dieta, ingesta, 200L, request);

		assertThat(result).isSameAs(dieta);
		assertThat(ingesta.getAlimentos()).isEmpty();
		assertThat(ingesta.getPlatillos()).hasSize(1);
		assertThat(ingesta.getPlatillos().get(0).getName()).isEqualTo("Combo");
		assertThat(ingesta.getPlatillos().get(0).getSourcePlatilloId()).isEqualTo(200L);
		assertThat(ingesta.getPlatillos().get(0).getIngredientes()).hasSize(2);
		verify(dietaService).saveDieta(dieta);
	}

	private static Alimento catalogAlimento(final Long id, final String nombre, final double cant, final int peso,
			final String unidad) {
		final Alimento alimento = new Alimento();
		alimento.setId(id);
		alimento.setNombreAlimento(nombre);
		alimento.setCantSugerida(cant);
		alimento.setPesoNeto(peso);
		alimento.setPesoBrutoRedondeado(peso);
		alimento.setUnidad(unidad);
		return alimento;
	}

	private static AlimentoIngesta alimentoIngesta(final Long id, final Alimento alimento, final int portions) {
		final AlimentoIngesta row = new AlimentoIngesta();
		row.setId(id);
		row.setAlimento(alimento);
		row.setName(alimento.getNombreAlimento());
		row.setUnidad(alimento.getUnidad());
		row.setPortions(portions);
		row.setPesoNeto(alimento.getPesoNeto() * portions);
		row.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado() * portions);
		return row;
	}

	private static Platillo savedPlatillo(final Long id, final String name) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName(name);
		return platillo;
	}

}
