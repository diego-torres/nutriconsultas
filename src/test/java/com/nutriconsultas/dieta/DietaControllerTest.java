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

	private Dieta dieta;

	private Ingesta ingesta;

	private Platillo platillo;

	private Ingrediente ingrediente;

	private Alimento alimento;

	@BeforeEach
	public void setup() {
		// Create test data
		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de prueba");

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
		when(platilloService.findById(1L)).thenReturn(platillo);

		List<Alimento> alimentos = new ArrayList<>();
		alimentos.add(alimento);
		when(alimentoService.findAll()).thenReturn(alimentos);
		when(alimentoService.findById(1L)).thenReturn(alimento);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDieta was called but saveDieta was not
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that platilloService.findById was called but saveDieta was not
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that platilloService.findById was never called
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService methods were called
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that dietaService.getDieta was called but saveDieta was not
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that alimentoService.findById was called but saveDieta was not
		verify(dietaService, times(1)).getDieta(1L);
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
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dietas/1"));

		// Verify that alimentoService.findById was never called
		verify(dietaService, times(1)).getDieta(1L);
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

}
