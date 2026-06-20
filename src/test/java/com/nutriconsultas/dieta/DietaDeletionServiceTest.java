package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class DietaDeletionServiceTest {

	private static final String NUTRITIONIST_USER_ID = "auth0|nutritionist";

	private static final String OTHER_USER_ID = "auth0|other-user";

	private static final String PLATFORM_ADMIN_USER_ID = "auth0|platform-admin";

	@Mock
	private DietaService dietaService;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private OidcUser nutritionistPrincipal;

	@Mock
	private OidcUser platformAdminPrincipal;

	private DietaDeletionServiceImpl service;

	@BeforeEach
	void setUp() {
		final DietaAuthorization dietaAuthorization = new DietaAuthorization(platformAdminService,
				platformAdminAuditService);
		service = new DietaDeletionServiceImpl(dietaService, dietaAuthorization, pacienteDietaRepository);
	}

	@Test
	void deleteDieta_deletesOwnedDietWhenNotAssigned() {
		final Dieta dieta = ownedDiet(5L);
		when(dietaService.getDietaByIdAndUserId(5L, NUTRITIONIST_USER_ID)).thenReturn(dieta);
		when(pacienteDietaRepository.countByDietaIdAndPacienteUserId(5L, NUTRITIONIST_USER_ID)).thenReturn(0L);

		final DietaDeleteResult result = service.deleteDieta(5L, NUTRITIONIST_USER_ID, nutritionistPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.DELETED);
		verify(dietaService).deleteDieta(5L);
	}

	@Test
	void deleteDieta_blocksWhenOwnedDietAssignedToPatients() {
		final Dieta dieta = ownedDiet(6L);
		when(dietaService.getDietaByIdAndUserId(6L, NUTRITIONIST_USER_ID)).thenReturn(dieta);
		when(pacienteDietaRepository.countByDietaIdAndPacienteUserId(6L, NUTRITIONIST_USER_ID)).thenReturn(2L);

		final DietaDeleteResult result = service.deleteDieta(6L, NUTRITIONIST_USER_ID, nutritionistPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.IN_USE);
		assertThat(result.getAssignedPatientCount()).isEqualTo(2L);
		verify(dietaService, never()).deleteDieta(6L);
	}

	@Test
	void deleteDieta_returnsForbiddenForOtherTenantDiet() {
		when(dietaService.getDietaByIdAndUserId(7L, NUTRITIONIST_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);
		final Dieta otherDiet = ownedDiet(7L);
		otherDiet.setUserId(OTHER_USER_ID);
		when(dietaService.getDieta(7L)).thenReturn(otherDiet);

		final DietaDeleteResult result = service.deleteDieta(7L, NUTRITIONIST_USER_ID, nutritionistPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.FORBIDDEN);
		verify(dietaService, never()).deleteDieta(7L);
	}

	@Test
	void deleteDieta_returnsNotFoundWhenDietMissing() {
		when(dietaService.getDietaByIdAndUserId(99L, NUTRITIONIST_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);
		when(dietaService.getDieta(99L)).thenReturn(null);

		final DietaDeleteResult result = service.deleteDieta(99L, NUTRITIONIST_USER_ID, nutritionistPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.NOT_FOUND);
		verify(dietaService, never()).deleteDieta(99L);
	}

	@Test
	void deleteDieta_allowsPlatformAdminToDeleteUnusedSystemTemplate() {
		final Dieta systemDieta = systemDiet(8L);
		when(dietaService.getDietaByIdAndUserId(8L, PLATFORM_ADMIN_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);
		when(platformAdminService.resolveActorUserId(platformAdminPrincipal)).thenReturn(PLATFORM_ADMIN_USER_ID);
		when(dietaService.getDieta(8L)).thenReturn(systemDieta);
		when(pacienteDietaRepository.countByDietaId(8L)).thenReturn(0L);

		final DietaDeleteResult result = service.deleteDieta(8L, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.DELETED);
		verify(dietaService).deleteDieta(8L);
		verify(platformAdminAuditService).recordAction(PLATFORM_ADMIN_USER_ID, "dietas.delete:dietaId=8");
	}

	@Test
	void deleteDieta_blocksPlatformAdminWhenSystemTemplateAssignedGlobally() {
		final Dieta systemDieta = systemDiet(9L);
		when(dietaService.getDietaByIdAndUserId(9L, PLATFORM_ADMIN_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);
		when(dietaService.getDieta(9L)).thenReturn(systemDieta);
		when(pacienteDietaRepository.countByDietaId(9L)).thenReturn(4L);

		final DietaDeleteResult result = service.deleteDieta(9L, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal);

		assertThat(result.getOutcome()).isEqualTo(DietaDeleteResult.Outcome.IN_USE);
		assertThat(result.getAssignedPatientCount()).isEqualTo(4L);
		verify(dietaService, never()).deleteDieta(9L);
	}

	@Test
	void countPatientAssignments_usesNutritionistScopeForOwnedDiet() {
		final Dieta dieta = ownedDiet(10L);

		service.countPatientAssignments(dieta, NUTRITIONIST_USER_ID);

		verify(pacienteDietaRepository).countByDietaIdAndPacienteUserId(10L, NUTRITIONIST_USER_ID);
		verify(pacienteDietaRepository, never()).countByDietaId(10L);
	}

	@Test
	void countPatientAssignments_usesGlobalScopeForSystemTemplate() {
		final Dieta systemDieta = systemDiet(11L);

		service.countPatientAssignments(systemDieta, PLATFORM_ADMIN_USER_ID);

		verify(pacienteDietaRepository).countByDietaId(11L);
		verify(pacienteDietaRepository, never()).countByDietaIdAndPacienteUserId(11L, PLATFORM_ADMIN_USER_ID);
	}

	private Dieta ownedDiet(final Long id) {
		final Dieta dieta = new Dieta();
		dieta.setId(id);
		dieta.setNombre("Dieta propia");
		dieta.setUserId(NUTRITIONIST_USER_ID);
		return dieta;
	}

	private Dieta systemDiet(final Long id) {
		final Dieta dieta = new Dieta();
		dieta.setId(id);
		dieta.setNombre("Plantilla sistema");
		dieta.setUserId(DietaCatalogConstants.SYSTEM_TEMPLATE_USER_ID);
		return dieta;
	}

}
