package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.model.ApiResponse;
import com.nutriconsultas.platillos.IngredienteFormModel;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class IngredientePlatilloIngestaRestControllerTest {

	@InjectMocks
	private IngredientePlatilloIngestaRestController controller;

	@Mock
	private DietaService dietaService;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private PacienteRepository pacienteRepository;

	private DietaAuthorization dietaAuthorization;

	private static final String TEST_USER_ID = "test-user-id-123";

	private Dieta dieta;

	private PlatilloIngesta platilloIngesta;

	private IngredientePlatilloIngesta ingrediente;

	@BeforeEach
	public void setup() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService,
				pacienteRepository);
		ReflectionTestUtils.setField(controller, "dietaAuthorization", dietaAuthorization);

		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setUserId(TEST_USER_ID);

		final Ingesta ingesta = new Ingesta();
		ingesta.setId(10L);
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());

		platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setId(20L);
		platilloIngesta.setPortions(1);
		platilloIngesta.setProteina(10.0);
		platilloIngesta.setIngesta(ingesta);
		platilloIngesta.setIngredientes(new ArrayList<>());

		final Alimento alimento = new Alimento();
		alimento.setId(5L);
		alimento.setNombreAlimento("Arroz");
		alimento.setCantSugerida(1.0);
		alimento.setUnidad("taza");
		alimento.setPesoNeto(100);
		alimento.setProteina(2.0);
		alimento.setEnergia(100);

		ingrediente = new IngredientePlatilloIngesta();
		ingrediente.setId(30L);
		ingrediente.setAlimento(alimento);
		ingrediente.setProteina(2.0);
		ingrediente.setPesoNeto(100);
		ingrediente.setPlatillo(platilloIngesta);
		platilloIngesta.getIngredientes().add(ingrediente);
		ingesta.getPlatillos().add(platilloIngesta);
		dieta.setIngestas(new ArrayList<>());
		dieta.getIngestas().add(ingesta);
	}

	private org.springframework.security.oauth2.core.oidc.user.OidcUser createMockOidcUser(final String userId) {
		final org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser = org.mockito.Mockito
			.mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
		org.mockito.Mockito.when(oidcUser.getSubject()).thenReturn(userId);
		return oidcUser;
	}

	@Test
	public void testDeleteIngredienteReturnsUpdatedDieta() {
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		final ResponseEntity<ApiResponse<Dieta>> result = controller.deleteIngrediente(1L, 10L, 20L, 30L,
				createMockOidcUser(TEST_USER_ID));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(dietaService).deleteIngredientePlatilloIngesta(platilloIngesta, 30L);
		verify(dietaService).saveDieta(dieta);
	}

	@Test
	public void testAddIngredienteReturnsUpdatedDieta() {
		final Alimento alimento = new Alimento();
		alimento.setId(7L);
		alimento.setNombreAlimento("Frijol");
		alimento.setCantSugerida(1.0);
		alimento.setUnidad("taza");
		alimento.setPesoNeto(90);
		alimento.setProteina(3.0);
		alimento.setEnergia(120);

		final IngredientePlatilloIngesta added = new IngredientePlatilloIngesta();
		added.setId(40L);
		added.setAlimento(alimento);

		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);
		when(dietaService.addIngredientePlatilloIngesta(platilloIngesta, 7L, "1", 90)).thenReturn(added);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		final IngredienteFormModel form = new IngredienteFormModel(7L, "1", 90);
		final ResponseEntity<ApiResponse<Dieta>> result = controller.addIngrediente(1L, 10L, 20L, form,
				createMockOidcUser(TEST_USER_ID));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(dietaService).addIngredientePlatilloIngesta(platilloIngesta, 7L, "1", 90);
	}

	@Test
	public void testUpdateIngredienteReturnsUpdatedDieta() {
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietaByIdAndUserId(1L, TEST_USER_ID)).thenReturn(dieta);
		when(dietaService.saveDieta(dieta)).thenReturn(dieta);

		final IngredienteFormModel form = new IngredienteFormModel(null, "2", 200);
		final ResponseEntity<ApiResponse<Dieta>> result = controller.updateIngrediente(1L, 10L, 20L, 30L, form,
				createMockOidcUser(TEST_USER_ID));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(dietaService).updateIngredientePlatilloIngesta(platilloIngesta, 30L, "2", 200);
	}

	@Test
	public void testToStringListIncludesInlineCantidadWhenEditable() {
		final java.util.List<String> row = controller.toStringList(ingrediente);

		assertThat(row.get(1)).contains("inline-platillo-ingesta-cantidad-input");
	}

}
