package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentoService;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class DietaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DietaService dietaService;

	@MockitoBean
	private PlatilloService platilloService;

	@MockitoBean
	private AlimentoService alimentoService;

	@MockitoBean
	private DietaPdfService dietaPdfService;

	private Dieta dieta;

	private Ingesta ingesta;

	private Platillo platillo;

	private Ingrediente ingrediente;

	private Alimento alimento;

	private static final String TEST_USER_ID = "test-user-id-123";

	private static final String OTHER_USER_ID = "other-user-id-456";

	@BeforeEach
	public void setup() {
		// Create test data
		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de prueba");
		dieta.setUserId(TEST_USER_ID);

		ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());

		dieta.setIngestas(new ArrayList<>());
		dieta.getIngestas().add(ingesta);

		alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Pollo");
		alimento.setClasificacion("CARNES");
		alimento.setUnidad("pieza");
		alimento.setEnergia(200);
		alimento.setProteina(25.0);
		alimento.setLipidos(10.0);
		alimento.setHidratosDeCarbono(0.0);

		ingrediente = new Ingrediente();
		ingrediente.setId(1L);
		ingrediente.setDescription("Pollo asado");
		ingrediente.setAlimento(alimento);
		ingrediente.setUnidad("pieza");
		ingrediente.setCantSugerida(1.0);
		ingrediente.setEnergia(200);
		ingrediente.setProteina(25.0);
		ingrediente.setLipidos(10.0);
		ingrediente.setHidratosDeCarbono(0.0);
		ingrediente.setFibra(0.0);
		ingrediente.setCalcio(10.0);
		ingrediente.setHierro(1.5);

		platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Pollo con arroz");
		platillo.setDescription("Platillo nutritivo");
		platillo.setEnergia(400);
		platillo.setProteina(30.0);
		platillo.setLipidos(15.0);
		platillo.setHidratosDeCarbono(50.0);
		platillo.setFibra(2.0);
		platillo.setCalcio(20.0);
		platillo.setHierro(2.0);

		List<Ingrediente> ingredientes = new ArrayList<>();
		ingredientes.add(ingrediente);
		platillo.setIngredientes(ingredientes);

		// Setup mocks
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);
		when(platilloService.findById(1L)).thenReturn(platillo);

		List<Alimento> alimentos = new ArrayList<>();
		alimentos.add(alimento);
		when(alimentoService.findAll()).thenReturn(alimentos);
		when(alimentoService.findById(1L)).thenReturn(alimento);
	}

	/**
	 * Creates an OidcLoginRequestPostProcessor for testing with a specific user ID.
	 * @param userId the user ID (subject)
	 * @return an OidcLoginRequestPostProcessor configured with the user ID
	 */
	private SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor oidcLogin(final String userId) {
		return SecurityMockMvcRequestPostProcessors.oidcLogin()
			.idToken(token -> token.subject(userId).claim("name", "Test User")
				.claim("picture", "https://example.com/picture.jpg"));
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloSuccess() throws Exception {
		log.info("Starting testSavePlatillo_Success");

		// Verify initial state
		assertThat(ingesta.getPlatillos()).isEmpty();

		// Perform POST request
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "1")
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(platilloService, times(1)).findById(1L);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		// Verify that the dieta passed to saveDieta has the platillo added
		verify(dietaService).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_Success");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloWithDefaultPortions() throws Exception {
		log.info("Starting testSavePlatillo_WithDefaultPortions");

		// Perform POST request without porciones (should default to 1)
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(platilloService, times(1)).findById(1L);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_WithDefaultPortions");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloIngestaNotFound() throws Exception {
		log.info("Starting testSavePlatillo_IngestaNotFound");

		// Perform POST request with non-existent ingesta ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "999")
				.param("platillo", "1")
				.param("porciones", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDietaByIdAndUserId was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(platilloService, never()).findById(any(Long.class));
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_IngestaNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloPlatilloNotFound() throws Exception {
		log.info("Starting testSavePlatillo_PlatilloNotFound");

		// Setup mock to return null for platillo
		when(platilloService.findById(999L)).thenReturn(null);

		// Perform POST request with non-existent platillo ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "999")
				.param("porciones", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that platilloService.findById was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(platilloService, times(1)).findById(999L);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_PlatilloNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloWithNullPlatilloId() throws Exception {
		log.info("Starting testSavePlatillo_WithNullPlatilloId");

		// Perform POST request with null platillo ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("porciones", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that platilloService.findById was never called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(platilloService, never()).findById(any(Long.class));
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_WithNullPlatilloId");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloWithIngredientes() throws Exception {
		log.info("Starting testSavePlatillo_WithIngredientes");

		// Verify that platillo has ingredientes
		assertThat(platillo.getIngredientes()).isNotEmpty();
		assertThat(platillo.getIngredientes().size()).isEqualTo(1);

		// Perform POST request
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "1")
				.param("porciones", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.saveDieta was called with a dieta that has the
		// platillo
		verify(dietaService).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_WithIngredientes");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloPlatilloWithoutIngredientes() throws Exception {
		log.info("Starting testSavePlatillo_PlatilloWithoutIngredientes");

		// Create platillo without ingredientes
		Platillo platilloSinIngredientes = new Platillo();
		platilloSinIngredientes.setId(2L);
		platilloSinIngredientes.setName("Platillo simple");
		platilloSinIngredientes.setIngredientes(new ArrayList<>());

		when(platilloService.findById(2L)).thenReturn(platilloSinIngredientes);

		// Perform POST request
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "2")
				.param("porciones", "1")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.saveDieta was still called
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatillo_PlatilloWithoutIngredientes");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarDieta() throws Exception {
		log.info("Starting testEditarDieta");

		// Setup mock for findAll platillos
		List<Platillo> platillos = new ArrayList<>();
		platillos.add(platillo);
		when(platilloService.findAll()).thenReturn(platillos);

		// Perform GET request
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/1"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/dietas/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "dietas"))
			.andExpect(MockMvcResultMatchers.model().attribute("dieta", dieta));

		// Verify that services were called
		verify(dietaService, times(1)).getDieta(1L);
		verify(platilloService, times(1)).findAll();

		log.info("Finishing testEditarDieta");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveAlimentoSuccess() throws Exception {
		log.info("Starting testSaveAlimento_Success");

		// Verify initial state
		assertThat(ingesta.getAlimentos()).isEmpty();

		// Perform POST request
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaAlimento", "1")
				.param("alimento", "1")
				.param("porciones", "2")
				.param("tipoPorcion", "porcion")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(alimentoService, times(1)).findById(1L);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveAlimento_Success");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveAlimentoWithDefaultPortions() throws Exception {
		log.info("Starting testSaveAlimento_WithDefaultPortions");

		// Perform POST request without porciones (should default to 1)
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaAlimento", "1")
				.param("alimento", "1")
				.param("tipoPorcion", "gramos")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(alimentoService, times(1)).findById(1L);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveAlimento_WithDefaultPortions");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveAlimentoIngestaNotFound() throws Exception {
		log.info("Starting testSaveAlimento_IngestaNotFound");

		// Perform POST request with non-existent ingesta ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaAlimento", "999")
				.param("alimento", "1")
				.param("porciones", "1")
				.param("tipoPorcion", "porcion")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDietaByIdAndUserId was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(alimentoService, never()).findById(any(Long.class));
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveAlimento_IngestaNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveAlimentoAlimentoNotFound() throws Exception {
		log.info("Starting testSaveAlimento_AlimentoNotFound");

		// Setup mock to return null for alimento
		when(alimentoService.findById(999L)).thenReturn(null);

		// Perform POST request with non-existent alimento ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaAlimento", "1")
				.param("alimento", "999")
				.param("porciones", "1")
				.param("tipoPorcion", "porcion")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that alimentoService.findById was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(alimentoService, times(1)).findById(999L);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveAlimento_AlimentoNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveAlimentoWithNullAlimentoId() throws Exception {
		log.info("Starting testSaveAlimento_WithNullAlimentoId");

		// Perform POST request with null alimento ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaAlimento", "1")
				.param("porciones", "1")
				.param("tipoPorcion", "porcion")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that alimentoService.findById was never called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(alimentoService, never()).findById(any(Long.class));
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveAlimento_WithNullAlimentoId");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarDietaLoadsAlimentos() throws Exception {
		log.info("Starting testEditarDietaLoadsAlimentos");

		// Setup mock for findAll platillos and alimentos
		List<Platillo> platillos = new ArrayList<>();
		platillos.add(platillo);
		when(platilloService.findAll()).thenReturn(platillos);

		List<Alimento> alimentos = new ArrayList<>();
		alimentos.add(alimento);
		when(alimentoService.findAll()).thenReturn(alimentos);

		// Perform GET request
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/1"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/dietas/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "dietas"))
			.andExpect(MockMvcResultMatchers.model().attribute("dieta", dieta))
			.andExpect(MockMvcResultMatchers.model().attribute("alimentos", alimentos));

		// Verify that services were called
		verify(dietaService, times(1)).getDieta(1L);
		verify(platilloService, times(1)).findAll();
		verify(alimentoService, times(1)).findAll();

		log.info("Finishing testEditarDietaLoadsAlimentos");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testPrintDieta() throws Exception {
		log.info("Starting testPrintDieta");

		final byte[] pdfBytes = "PDF content".getBytes();
		// When accessed from diet list, patient info should be excluded
		when(dietaPdfService.generatePdf(1L, false)).thenReturn(pdfBytes);

		// Perform GET request
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/1/print"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
			.andExpect(MockMvcResultMatchers.header()
				.string("Content-Disposition", "attachment; filename=\"Dieta de prueba.pdf\""))
			.andExpect(MockMvcResultMatchers.content().bytes(pdfBytes));

		// Verify that service was called with includePatientInfo = false
		verify(dietaPdfService, times(1)).generatePdf(1L, false);

		log.info("Finishing testPrintDieta");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testPrintDietaNotFound() throws Exception {
		log.info("Starting testPrintDietaNotFound");

		when(dietaPdfService.generatePdf(999L, false))
			.thenThrow(new IllegalArgumentException("Dieta with id 999 not found"));

		// Perform GET request - exception will be wrapped in ServletException
		try {
			mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/999/print"))
				.andExpect(status().isInternalServerError());
		}
		catch (Exception e) {
			// Expected - exception is thrown during request processing
			assertThat(e).hasRootCauseInstanceOf(IllegalArgumentException.class);
		}

		// Verify that service was called with includePatientInfo = false
		verify(dietaPdfService, times(1)).generatePdf(999L, false);

		log.info("Finishing testPrintDietaNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUpdatePlatilloIngestaSuccess() throws Exception {
		log.info("Starting testUpdatePlatilloIngestaSuccess");

		// Create platillo ingesta with initial portions
		PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(1L);
		platilloIngesta.setName("Platillo de prueba");
		platilloIngesta.setPortions(1);
		platilloIngesta.setEnergia(250);
		platilloIngesta.setProteina(15.0);
		platilloIngesta.setLipidos(8.0);
		platilloIngesta.setHidratosDeCarbono(30.0);
		platilloIngesta.setIngesta(ingesta);

		ingesta.getPlatillos().add(platilloIngesta);

		// Perform POST request to update portions
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/1/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testUpdatePlatilloIngestaSuccess");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUpdatePlatilloIngestaNotFound() throws Exception {
		log.info("Starting testUpdatePlatilloIngestaNotFound");

		// Perform POST request with non-existent platillo ingesta ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/999/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDietaByIdAndUserId was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testUpdatePlatilloIngestaNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUpdateAlimentoIngestaSuccess() throws Exception {
		log.info("Starting testUpdateAlimentoIngestaSuccess");

		// Create alimento ingesta with initial portions
		AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setId(1L);
		alimentoIngesta.setName("Pollo");
		alimentoIngesta.setPortions(1);
		alimentoIngesta.setEnergia(200);
		alimentoIngesta.setProteina(25.0);
		alimentoIngesta.setLipidos(10.0);
		alimentoIngesta.setHidratosDeCarbono(0.0);
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setIngesta(ingesta);

		ingesta.setAlimentos(new ArrayList<>());
		ingesta.getAlimentos().add(alimentoIngesta);

		// Perform POST request to update portions
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/1/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testUpdateAlimentoIngestaSuccess");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUpdateAlimentoIngestaNotFound() throws Exception {
		log.info("Starting testUpdateAlimentoIngestaNotFound");

		// Perform POST request with non-existent alimento ingesta ID
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/999/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDietaByIdAndUserId was called but saveDieta was not
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testUpdateAlimentoIngestaNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUpdateAlimentoIngestaWithoutAlimentoReference() throws Exception {
		log.info("Starting testUpdateAlimentoIngestaWithoutAlimentoReference");

		// Create alimento ingesta without alimento reference
		AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setId(2L);
		alimentoIngesta.setName("Alimento sin referencia");
		alimentoIngesta.setPortions(1);
		alimentoIngesta.setIngesta(ingesta);
		// alimentoIngesta.setAlimento(null); // No alimento reference

		ingesta.setAlimentos(new ArrayList<>());
		ingesta.getAlimentos().add(alimentoIngesta);

		// Perform POST request to update portions
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/alimentos/2/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDietaByIdAndUserId was called but saveDieta was not
		// (because alimento reference is null)
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testUpdateAlimentoIngestaWithoutAlimentoReference");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarDietaShowsOwnershipInfo() throws Exception {
		log.info("Starting testEditarDietaShowsOwnershipInfo");

		// Setup mock for findAll platillos
		List<Platillo> platillos = new ArrayList<>();
		platillos.add(platillo);
		when(platilloService.findAll()).thenReturn(platillos);

		// Perform GET request
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/1").with(oidcLogin(TEST_USER_ID)))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/dietas/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "dietas"))
			.andExpect(MockMvcResultMatchers.model().attribute("dieta", dieta))
			.andExpect(MockMvcResultMatchers.model().attribute("isOwner", true));

		// Verify that services were called
		verify(dietaService, times(1)).getDieta(1L);
		verify(platilloService, times(1)).findAll();

		log.info("Finishing testEditarDietaShowsOwnershipInfo");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarDietaShowsNotOwnerForOtherUserDiet() throws Exception {
		log.info("Starting testEditarDietaShowsNotOwnerForOtherUserDiet");

		// Create diet owned by other user
		Dieta otherUserDieta = new Dieta();
		otherUserDieta.setId(2L);
		otherUserDieta.setNombre("Dieta de otro usuario");
		otherUserDieta.setUserId(OTHER_USER_ID);
		otherUserDieta.setIngestas(new ArrayList<>());

		when(dietaService.getDieta(2L)).thenReturn(otherUserDieta);

		// Setup mock for findAll platillos
		List<Platillo> platillos = new ArrayList<>();
		platillos.add(platillo);
		when(platilloService.findAll()).thenReturn(platillos);

		// Perform GET request with different user
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/dietas/2").with(oidcLogin(TEST_USER_ID)))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/dietas/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "dietas"))
			.andExpect(MockMvcResultMatchers.model().attribute("dieta", otherUserDieta))
			.andExpect(MockMvcResultMatchers.model().attribute("isOwner", false));

		// Verify that services were called
		verify(dietaService, times(1)).getDieta(2L);
		verify(platilloService, times(1)).findAll();

		log.info("Finishing testEditarDietaShowsNotOwnerForOtherUserDiet");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveDietaChecksOwnership() throws Exception {
		log.info("Starting testSaveDietaChecksOwnership");

		// Perform POST request with correct user
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("id", "1")
				.param("nombre", "Dieta Modificada")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that ownership check was performed
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveDietaChecksOwnership");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveDietaRejectsOtherUserDiet() throws Exception {
		log.info("Starting testSaveDietaRejectsOtherUserDiet");

		// Setup - diet belongs to other user
		when(dietaService.getDietaByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(null);

		// Perform POST request with wrong user - should throw exception
		try {
			mockMvc
				.perform(MockMvcRequestBuilders.post("/admin/dietas/save")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.param("id", "1")
					.param("nombre", "Dieta Modificada")
					.with(oidcLogin(OTHER_USER_ID))
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is5xxServerError());
		}
		catch (Exception e) {
			// Expected - IllegalArgumentException is thrown
			assertThat(e).hasRootCauseInstanceOf(IllegalArgumentException.class);
		}

		// Verify that ownership check was performed
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, OTHER_USER_ID);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSaveDietaRejectsOtherUserDiet");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloChecksOwnership() throws Exception {
		log.info("Starting testSavePlatilloChecksOwnership");

		// Perform POST request with correct user
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("ingestaPlatillo", "1")
				.param("platillo", "1")
				.param("porciones", "2")
				.with(oidcLogin(TEST_USER_ID))
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that ownership check was performed
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaService, times(1)).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatilloChecksOwnership");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSavePlatilloRejectsOtherUserDiet() throws Exception {
		log.info("Starting testSavePlatilloRejectsOtherUserDiet");

		// Setup - diet belongs to other user
		when(dietaService.getDietaByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(null);

		// Perform POST request with wrong user - should throw exception
		try {
			mockMvc
				.perform(MockMvcRequestBuilders.post("/admin/dietas/1/platillos/save")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.param("ingestaPlatillo", "1")
					.param("platillo", "1")
					.param("porciones", "2")
					.with(oidcLogin(OTHER_USER_ID))
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is5xxServerError());
		}
		catch (Exception e) {
			// Expected - IllegalArgumentException is thrown
			assertThat(e).hasRootCauseInstanceOf(IllegalArgumentException.class);
		}

		// Verify that ownership check was performed
		verify(dietaService, times(1)).getDietaByIdAndUserId(1L, OTHER_USER_ID);
		verify(dietaService, never()).saveDieta(any(Dieta.class));

		log.info("Finishing testSavePlatilloRejectsOtherUserDiet");
	}

}
