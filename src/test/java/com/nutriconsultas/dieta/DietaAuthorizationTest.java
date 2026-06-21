package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

@ExtendWith(MockitoExtension.class)
class DietaAuthorizationTest {

	private static final String NUTRITIONIST_USER_ID = "auth0|nutritionist-one";

	private static final String PLATFORM_ADMIN_USER_ID = "auth0|platform-admin-test";

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private DietaService dietaService;

	@Mock
	private OidcUser nutritionistPrincipal;

	@Mock
	private OidcUser platformAdminPrincipal;

	private DietaAuthorization dietaAuthorization;

	@Test
	void canModify_returnsTrueForOwnedDiet() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta dieta = ownedDiet(10L, NUTRITIONIST_USER_ID);

		assertThat(dietaAuthorization.canModify(dieta, NUTRITIONIST_USER_ID, nutritionistPrincipal)).isTrue();
	}

	@Test
	void canModify_returnsTrueForPlatformAdminOnSystemTemplate() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta dieta = systemDiet(8L);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);

		assertThat(dietaAuthorization.canModify(dieta, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal)).isTrue();
	}

	@Test
	void canModify_returnsFalseForNutritionistOnSystemTemplate() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta dieta = systemDiet(8L);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		assertThat(dietaAuthorization.canModify(dieta, NUTRITIONIST_USER_ID, nutritionistPrincipal)).isFalse();
	}

	@Test
	void canModify_returnsFalseForPlatformAdminOnOtherUserDiet() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta dieta = ownedDiet(11L, "auth0|other-user");
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);

		assertThat(dietaAuthorization.canModify(dieta, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal)).isFalse();
	}

	@Test
	void verifyCanModify_throwsForUnauthorizedUser() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta dieta = systemDiet(8L);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		assertThatThrownBy(() -> dietaAuthorization.verifyCanModify(dieta, NUTRITIONIST_USER_ID, nutritionistPrincipal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No tiene permiso para modificar esta dieta");
	}

	@Test
	void resolveForMutation_returnsOwnedDietWithoutAdminCheck() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta owned = ownedDiet(5L, NUTRITIONIST_USER_ID);
		when(dietaService.getDietaByIdAndUserId(5L, NUTRITIONIST_USER_ID)).thenReturn(owned);

		final Dieta result = dietaAuthorization.resolveForMutation(5L, NUTRITIONIST_USER_ID, nutritionistPrincipal,
				dietaService);

		assertThat(result).isSameAs(owned);
		verify(platformAdminService, never()).isPlatformAdmin(nutritionistPrincipal);
	}

	@Test
	void resolveForMutation_returnsSystemDietForPlatformAdmin() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta system = systemDiet(8L);
		when(dietaService.getDietaByIdAndUserId(8L, PLATFORM_ADMIN_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);
		when(dietaService.getDieta(8L)).thenReturn(system);

		final Dieta result = dietaAuthorization.resolveForMutation(8L, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal,
				dietaService);

		assertThat(result).isSameAs(system);
	}

	@Test
	void resolveForMutation_returnsNullForNutritionistOnSystemDiet() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		when(dietaService.getDietaByIdAndUserId(8L, NUTRITIONIST_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		final Dieta result = dietaAuthorization.resolveForMutation(8L, NUTRITIONIST_USER_ID, nutritionistPrincipal,
				dietaService);

		assertThat(result).isNull();
	}

	@Test
	void auditSystemDietMutationIfNeeded_recordsPlatformAdminAction() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta system = systemDiet(8L);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);
		when(platformAdminService.resolveActorUserId(platformAdminPrincipal)).thenReturn(PLATFORM_ADMIN_USER_ID);

		dietaAuthorization.auditSystemDietMutationIfNeeded(platformAdminPrincipal, system, "dietas.save");

		verify(platformAdminAuditService).recordAction(PLATFORM_ADMIN_USER_ID, "dietas.save:dietaId=8");
	}

	@Test
	void auditSystemDietMutationIfNeeded_skipsNonSystemDiet() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		final Dieta owned = ownedDiet(5L, NUTRITIONIST_USER_ID);

		dietaAuthorization.auditSystemDietMutationIfNeeded(platformAdminPrincipal, owned, "dietas.save");

		verify(platformAdminAuditService, never()).recordAction(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void resolveCreateUserId_returnsSystemTemplateUserIdForPlatformAdmin() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);

		assertThat(dietaAuthorization.resolveCreateUserId(platformAdminPrincipal, PLATFORM_ADMIN_USER_ID))
			.isEqualTo(DietaCatalogConstants.SYSTEM_TEMPLATE_USER_ID);
	}

	@Test
	void resolveCreateUserId_returnsOAuthUserIdForNutritionist() {
		dietaAuthorization = new DietaAuthorization(platformAdminService, platformAdminAuditService);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		assertThat(dietaAuthorization.resolveCreateUserId(nutritionistPrincipal, NUTRITIONIST_USER_ID))
			.isEqualTo(NUTRITIONIST_USER_ID);
	}

	private static Dieta ownedDiet(final Long id, final String userId) {
		final Dieta dieta = new Dieta();
		dieta.setId(id);
		dieta.setNombre("Dieta propia");
		dieta.setUserId(userId);
		return dieta;
	}

	private static Dieta systemDiet(final Long id) {
		final Dieta dieta = new Dieta();
		dieta.setId(id);
		dieta.setNombre("Plantilla: Menú vegetal 02");
		dieta.setUserId(DietaCatalogConstants.SYSTEM_TEMPLATE_USER_ID);
		return dieta;
	}

}
