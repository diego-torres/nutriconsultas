package com.nutriconsultas.platillos;

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
class PlatilloAuthorizationTest {

	private static final String NUTRITIONIST_USER_ID = "auth0|nutritionist-one";

	private static final String PLATFORM_ADMIN_USER_ID = "auth0|platform-admin-test";

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private PlatilloService platilloService;

	@Mock
	private OidcUser nutritionistPrincipal;

	@Mock
	private OidcUser platformAdminPrincipal;

	private PlatilloAuthorization platilloAuthorization;

	@Test
	void canModify_returnsTrueForOwner() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo platillo = ownedPlatillo(10L, NUTRITIONIST_USER_ID);

		assertThat(platilloAuthorization.canModify(platillo, NUTRITIONIST_USER_ID, nutritionistPrincipal)).isTrue();
	}

	@Test
	void canModify_returnsFalseForNonOwnerOnOwnedPlatillo() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo platillo = ownedPlatillo(10L, "auth0|other-nutritionist");

		assertThat(platilloAuthorization.canModify(platillo, NUTRITIONIST_USER_ID, nutritionistPrincipal)).isFalse();
	}

	@Test
	void canModify_returnsTrueForPlatformAdminOnSystemCatalog() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo platillo = systemPlatillo(97L);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);

		assertThat(platilloAuthorization.canModify(platillo, PLATFORM_ADMIN_USER_ID, platformAdminPrincipal)).isTrue();
	}

	@Test
	void canModify_returnsFalseForNutritionistOnSystemCatalog() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo platillo = systemPlatillo(97L);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		assertThat(platilloAuthorization.canModify(platillo, NUTRITIONIST_USER_ID, nutritionistPrincipal)).isFalse();
	}

	@Test
	void canCopy_returnsTrueForSystemCatalog() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);

		assertThat(platilloAuthorization.canCopy(systemPlatillo(97L), NUTRITIONIST_USER_ID)).isTrue();
	}

	@Test
	void canCopy_returnsTrueForOwner() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);

		assertThat(platilloAuthorization.canCopy(ownedPlatillo(10L, NUTRITIONIST_USER_ID), NUTRITIONIST_USER_ID))
			.isTrue();
	}

	@Test
	void canCopy_returnsFalseForOtherNutritionistOwnedPlatillo() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);

		assertThat(platilloAuthorization.canCopy(ownedPlatillo(10L, "auth0|other-nutritionist"), NUTRITIONIST_USER_ID))
			.isFalse();
	}

	@Test
	void verifyCanModify_throwsForUnauthorizedUser() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo platillo = systemPlatillo(97L);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		assertThatThrownBy(
				() -> platilloAuthorization.verifyCanModify(platillo, NUTRITIONIST_USER_ID, nutritionistPrincipal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No tiene permiso para modificar este platillo");
	}

	@Test
	void resolveForMutation_returnsOwnedPlatilloForCreator() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo owned = ownedPlatillo(5L, NUTRITIONIST_USER_ID);
		when(platilloService.findByIdAndUserId(5L, NUTRITIONIST_USER_ID)).thenReturn(owned);

		final Platillo result = platilloAuthorization.resolveForMutation(5L, NUTRITIONIST_USER_ID,
				nutritionistPrincipal, platilloService);

		assertThat(result).isSameAs(owned);
	}

	@Test
	void resolveForMutation_returnsNullForNonOwnerOnOwnedPlatillo() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		when(platilloService.findByIdAndUserId(5L, NUTRITIONIST_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		final Platillo result = platilloAuthorization.resolveForMutation(5L, NUTRITIONIST_USER_ID,
				nutritionistPrincipal, platilloService);

		assertThat(result).isNull();
	}

	@Test
	void resolveForMutation_returnsSystemPlatilloForPlatformAdmin() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo system = systemPlatillo(97L);
		when(platilloService.findByIdAndUserId(97L, PLATFORM_ADMIN_USER_ID)).thenReturn(null);
		when(platilloService.findById(97L)).thenReturn(system);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);

		final Platillo result = platilloAuthorization.resolveForMutation(97L, PLATFORM_ADMIN_USER_ID,
				platformAdminPrincipal, platilloService);

		assertThat(result).isSameAs(system);
	}

	@Test
	void resolveForMutation_returnsNullForNutritionistOnSystemPlatillo() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		when(platilloService.findByIdAndUserId(97L, NUTRITIONIST_USER_ID)).thenReturn(null);
		when(platformAdminService.isPlatformAdmin(nutritionistPrincipal)).thenReturn(false);

		final Platillo result = platilloAuthorization.resolveForMutation(97L, NUTRITIONIST_USER_ID,
				nutritionistPrincipal, platilloService);

		assertThat(result).isNull();
	}

	@Test
	void auditSystemPlatilloMutationIfNeeded_recordsPlatformAdminAction() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo system = systemPlatillo(97L);
		when(platformAdminService.isPlatformAdmin(platformAdminPrincipal)).thenReturn(true);
		when(platformAdminService.resolveActorUserId(platformAdminPrincipal)).thenReturn(PLATFORM_ADMIN_USER_ID);

		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(platformAdminPrincipal, system, "platillos.save");

		verify(platformAdminAuditService).recordAction(PLATFORM_ADMIN_USER_ID, "platillos.save:platilloId=97");
	}

	@Test
	void auditSystemPlatilloMutationIfNeeded_skipsNonSystemPlatillo() {
		platilloAuthorization = new PlatilloAuthorization(platformAdminService, platformAdminAuditService);
		final Platillo owned = ownedPlatillo(5L, NUTRITIONIST_USER_ID);

		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(platformAdminPrincipal, owned, "platillos.save");

		verify(platformAdminAuditService, never()).recordAction(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	private static Platillo ownedPlatillo(final Long id, final String userId) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName("Platillo propio");
		platillo.setUserId(userId);
		return platillo;
	}

	private static Platillo systemPlatillo(final Long id) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName("Frijoles con tortilla");
		platillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		return platillo;
	}

}
