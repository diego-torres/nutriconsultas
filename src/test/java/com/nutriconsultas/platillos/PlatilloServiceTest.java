package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlatilloServiceTest {

	private static final String TEST_USER_ID = "auth0|nutritionist-one";

	@InjectMocks
	private PlatilloServiceImpl platilloService;

	@Mock
	private PlatilloRepository platilloRepository;

	@Test
	void getPlatillosForCatalogFilter_todas_returnsSystemAndOwned() {
		final Platillo system = systemPlatillo(1L);
		final Platillo owned = ownedPlatillo(2L, TEST_USER_ID);
		when(platilloRepository.findByUserIdIn(List.of(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID, TEST_USER_ID)))
			.thenReturn(List.of(system, owned));

		final List<Platillo> result = platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.TODAS,
				TEST_USER_ID);

		assertThat(result).containsExactly(system, owned);
	}

	@Test
	void getPlatillosForCatalogFilter_sistema_returnsSystemOnly() {
		final Platillo system = systemPlatillo(1L);
		when(platilloRepository.findByUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID))
			.thenReturn(List.of(system));

		final List<Platillo> result = platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.SISTEMA,
				TEST_USER_ID);

		assertThat(result).containsExactly(system);
	}

	@Test
	void getPlatillosForCatalogFilter_propias_returnsOwnedOnly() {
		final Platillo owned = ownedPlatillo(2L, TEST_USER_ID);
		when(platilloRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(owned));

		final List<Platillo> result = platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.PROPIAS,
				TEST_USER_ID);

		assertThat(result).containsExactly(owned);
	}

	@Test
	void getPlatillosForCatalogFilter_propias_withoutUserId_returnsEmpty() {
		final List<Platillo> result = platilloService.getPlatillosForCatalogFilter(PlatilloCatalogFilter.PROPIAS, null);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdAndUserId_delegatesToRepository() {
		final Platillo owned = ownedPlatillo(5L, TEST_USER_ID);
		when(platilloRepository.findByIdAndUserId(5L, TEST_USER_ID)).thenReturn(java.util.Optional.of(owned));

		assertThat(platilloService.findByIdAndUserId(5L, TEST_USER_ID)).isSameAs(owned);
	}

	@Test
	void deletePlatillo_delegatesToRepository() {
		platilloService.deletePlatillo(9L);

		verify(platilloRepository).deleteById(9L);
	}

	private static Platillo systemPlatillo(final Long id) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName("System platillo");
		platillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		return platillo;
	}

	private static Platillo ownedPlatillo(final Long id, final String userId) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName("Owned platillo");
		platillo.setUserId(userId);
		return platillo;
	}

}
