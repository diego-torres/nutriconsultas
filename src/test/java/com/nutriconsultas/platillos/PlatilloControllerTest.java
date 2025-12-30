package com.nutriconsultas.platillos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;

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

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PlatilloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlatilloService platilloService;

	@MockitoBean
	private AlimentoService alimentoService;

	private Platillo platillo;

	private Alimento alimento;

	@BeforeEach
	public void setup() {
		log.info("setting up PlatilloController test");

		platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Test Platillo");
		platillo.setDescription("Test Description");
		platillo.setIngestasSugeridas("Desayuno,Comida");

		alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Test Alimento");

		log.info("finished setting up PlatilloController test");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testNuevo() throws Exception {
		log.info("Starting testNuevo");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos/nuevo"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "platillos"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("platillo"));
		log.info("Finishing testNuevo");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testListado() throws Exception {
		log.info("Starting testListado");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/listado"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "platillos"));
		log.info("Finishing testListado");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditar() throws Exception {
		log.info("Starting testEditar");
		when(platilloService.findById(1L)).thenReturn(platillo);
		when(alimentoService.findAll()).thenReturn(Arrays.asList(alimento));

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos/1"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "platillos"))
			.andExpect(MockMvcResultMatchers.model().attribute("platillo", platillo))
			.andExpect(MockMvcResultMatchers.model().attributeExists("ingestas"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("alimentosList"));
		log.info("Finishing testEditar");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarWithNullIngestas() throws Exception {
		log.info("Starting testEditarWithNullIngestas");
		platillo.setIngestasSugeridas(null);
		when(platilloService.findById(1L)).thenReturn(platillo);
		when(alimentoService.findAll()).thenReturn(new ArrayList<>());

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos/1"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "platillos"));
		log.info("Finishing testEditarWithNullIngestas");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveNewPlatillo() throws Exception {
		log.info("Starting testSaveNewPlatillo");
		Platillo newPlatillo = new Platillo();
		newPlatillo.setId(0L);
		newPlatillo.setName("New Platillo");
		newPlatillo.setDescription("New Description");

		when(platilloService.findById(0L)).thenReturn(null);
		when(platilloService.save(any(Platillo.class))).thenReturn(newPlatillo);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("id", "0")
				.param("name", "New Platillo")
				.param("description", "New Description")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/platillos/0"));

		verify(platilloService).save(any(Platillo.class));
		log.info("Finishing testSaveNewPlatillo");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveExistingPlatillo() throws Exception {
		log.info("Starting testSaveExistingPlatillo");
		when(platilloService.findById(1L)).thenReturn(platillo);
		when(platilloService.save(any(Platillo.class))).thenReturn(platillo);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("id", "1")
				.param("name", "Updated Platillo")
				.param("description", "Updated Description")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/platillos/1"));

		verify(platilloService).findById(1L);
		verify(platilloService).save(any(Platillo.class));
		log.info("Finishing testSaveExistingPlatillo");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testSaveWithNullId() throws Exception {
		log.info("Starting testSaveWithNullId");
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/platillos/save")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Test Platillo")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/platillos"));
		log.info("Finishing testSaveWithNullId");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUploadPicture() throws Exception {
		log.info("Starting testUploadPicture");
		byte[] imageBytes = "fake image data".getBytes();
		when(platilloService.findById(1L)).thenReturn(platillo);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/admin/platillos/1/picture")
			.file("imgPlatillo", imageBytes)
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.with(request -> {
				request.setMethod("POST");
				return request;
			}))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/platillos/1"));

		verify(platilloService).savePicture(eq(1L), any(byte[].class), anyString());
		log.info("Finishing testUploadPicture");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUploadPictureEmptyFile() throws Exception {
		log.info("Starting testUploadPictureEmptyFile");
		when(platilloService.findById(1L)).thenReturn(platillo);
		when(alimentoService.findAll()).thenReturn(new ArrayList<>());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/admin/platillos/1/picture")
			.file("imgPlatillo", new byte[0])
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.with(request -> {
				request.setMethod("POST");
				return request;
			}))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("errorMessage", "The file is empty"));
		log.info("Finishing testUploadPictureEmptyFile");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testGetImage() throws Exception {
		log.info("Starting testGetImage");
		byte[] imageBytes = "fake image data".getBytes();
		when(platilloService.getPicture(1L, "test.jpg")).thenReturn(imageBytes);

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos/platillo/1/test.jpg"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG))
			.andExpect(MockMvcResultMatchers.content().bytes(imageBytes));

		verify(platilloService).getPicture(1L, "test.jpg");
		log.info("Finishing testGetImage");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUploadPdf() throws Exception {
		log.info("Starting testUploadPdf");
		byte[] pdfBytes = "fake pdf data".getBytes();
		when(platilloService.findById(1L)).thenReturn(platillo);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/admin/platillos/1/pdf")
			.file("pdfPlatillo", pdfBytes)
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.with(request -> {
				request.setMethod("POST");
				return request;
			}))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/platillos/1"));

		verify(platilloService).savePdf(eq(1L), any(byte[].class));
		log.info("Finishing testUploadPdf");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testUploadPdfEmptyFile() throws Exception {
		log.info("Starting testUploadPdfEmptyFile");
		when(platilloService.findById(1L)).thenReturn(platillo);
		when(alimentoService.findAll()).thenReturn(new ArrayList<>());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/admin/platillos/1/pdf")
			.file("pdfPlatillo", new byte[0])
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.with(request -> {
				request.setMethod("POST");
				return request;
			}))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/platillos/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("errorMessage", "The file is empty"));
		log.info("Finishing testUploadPdfEmptyFile");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testGetPdf() throws Exception {
		log.info("Starting testGetPdf");
		byte[] pdfBytes = "fake pdf data".getBytes();
		when(platilloService.getPicture(1L, "instrucciones.pdf")).thenReturn(pdfBytes);

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/platillos/platillo/1/instrucciones.pdf"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
			.andExpect(MockMvcResultMatchers.content().bytes(pdfBytes));

		verify(platilloService).getPicture(1L, "instrucciones.pdf");
		log.info("Finishing testGetPdf");
	}

}
