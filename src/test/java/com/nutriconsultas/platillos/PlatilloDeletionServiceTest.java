package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.dieta.PlatilloIngestaRepository;

@ExtendWith(MockitoExtension.class)
class PlatilloDeletionServiceTest {

	private static final String OWNER_USER_ID = "auth0|nutritionist-one";

	private static final String OTHER_USER_ID = "auth0|nutritionist-two";

	@InjectMocks
	private PlatilloDeletionServiceImpl platilloDeletionService;

	@Mock
	private PlatilloService platilloService;

	@Mock
	private PlatilloAuthorization platilloAuthorization;

	@Mock
	private PlatilloIngestaRepository platilloIngestaRepository;

	@Mock
	private OidcUser ownerPrincipal;

	@Test
	void deletePlatillo_deletesOwnedPlatilloWhenNotReferenced() {
		final Platillo owned = ownedPlatillo(5L, OWNER_USER_ID);
		when(platilloAuthorization.resolveForMutation(5L, OWNER_USER_ID, ownerPrincipal, platilloService))
			.thenReturn(owned);
		when(platilloAuthorization.canModify(owned, OWNER_USER_ID, ownerPrincipal)).thenReturn(true);
		when(platilloIngestaRepository.countBySourcePlatilloIdAndDietaUserId(5L, OWNER_USER_ID)).thenReturn(0L);

		final PlatilloDeleteResult result = platilloDeletionService.deletePlatillo(5L, OWNER_USER_ID, ownerPrincipal);

		assertThat(result.getOutcome()).isEqualTo(PlatilloDeleteResult.Outcome.DELETED);
		verify(platilloService).deletePlatillo(5L);
	}

	@Test
	void deletePlatillo_forbiddenForNonOwner() {
		when(platilloAuthorization.resolveForMutation(5L, OTHER_USER_ID, ownerPrincipal, platilloService))
			.thenReturn(null);
		when(platilloService.findById(5L)).thenReturn(ownedPlatillo(5L, OWNER_USER_ID));

		final PlatilloDeleteResult result = platilloDeletionService.deletePlatillo(5L, OTHER_USER_ID, ownerPrincipal);

		assertThat(result.getOutcome()).isEqualTo(PlatilloDeleteResult.Outcome.FORBIDDEN);
		verify(platilloService, never()).deletePlatillo(5L);
	}

	@Test
	void deletePlatillo_blockedWhenReferencedInDiets() {
		final Platillo owned = ownedPlatillo(5L, OWNER_USER_ID);
		when(platilloAuthorization.resolveForMutation(5L, OWNER_USER_ID, ownerPrincipal, platilloService))
			.thenReturn(owned);
		when(platilloAuthorization.canModify(owned, OWNER_USER_ID, ownerPrincipal)).thenReturn(true);
		when(platilloIngestaRepository.countBySourcePlatilloIdAndDietaUserId(5L, OWNER_USER_ID)).thenReturn(2L);

		final PlatilloDeleteResult result = platilloDeletionService.deletePlatillo(5L, OWNER_USER_ID, ownerPrincipal);

		assertThat(result.getOutcome()).isEqualTo(PlatilloDeleteResult.Outcome.IN_USE);
		assertThat(result.getDietReferenceCount()).isEqualTo(2L);
		verify(platilloService, never()).deletePlatillo(5L);
	}

	private static Platillo ownedPlatillo(final Long id, final String userId) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName("Owned platillo");
		platillo.setUserId(userId);
		return platillo;
	}

}
