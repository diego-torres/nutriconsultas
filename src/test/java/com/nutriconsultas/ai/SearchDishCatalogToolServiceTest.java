package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloRepository;

@ExtendWith(MockitoExtension.class)
class SearchDishCatalogToolServiceTest {

	private static final String NUTRITIONIST_A = "auth0|nutritionist-a";

	private static final String NUTRITIONIST_B = "auth0|nutritionist-b";

	@InjectMocks
	private SearchDishCatalogToolServiceImpl service;

	@Mock
	private PlatilloRepository platilloRepository;

	@Test
	void searchReturnsMappedItemsWithDefaultLimit() {
		final Platillo systemDish = samplePlatillo(1L, "Ensalada verde",
				PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID, "Comida", 120, 4.0);
		when(platilloRepository.findForAuthorizedCatalogSearch(any(), eq("%ensalada%"), eq(null), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(systemDish)));

		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "ensalada", null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().items()).hasSize(1);
		assertThat(result.data().items().get(0).platilloId()).isEqualTo(1L);
		assertThat(result.data().items().get(0).name()).isEqualTo("Ensalada verde");
		assertThat(result.data().items().get(0).ingestasSugeridas()).isEqualTo("Comida");
		assertThat(result.data().items().get(0).energiaKcal()).isEqualTo(120);
		assertThat(result.data().items().get(0).proteinaG()).isEqualTo(4.0);
		assertThat(result.data().items().get(0).systemCatalog()).isTrue();
		assertThat(result.data().items().get(0).ownedByNutritionist()).isFalse();
		assertThat(result.data().totalReturned()).isEqualTo(1);

		final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(platilloRepository).findForAuthorizedCatalogSearch(any(), eq("%ensalada%"), eq(null),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(SearchDishCatalogToolServiceImpl.DEFAULT_LIMIT);
	}

	@Test
	void searchScopesToSystemCatalogAndAuthenticatedNutritionist() {
		when(platilloRepository.findForAuthorizedCatalogSearch(any(), any(), any(), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		service.search(NUTRITIONIST_A, "tacos", null, 5);

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<List<String>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(platilloRepository).findForAuthorizedCatalogSearch(userIdsCaptor.capture(), eq("%tacos%"), eq(null),
				any(Pageable.class));
		assertThat(userIdsCaptor.getValue()).containsExactly(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID,
				NUTRITIONIST_A);
		assertThat(userIdsCaptor.getValue()).doesNotContain(NUTRITIONIST_B);
	}

	@Test
	void searchMarksOwnedNutritionistPlatillos() {
		final Platillo ownedDish = samplePlatillo(2L, "Tacos de pollo", NUTRITIONIST_A, "Cena", 300, 18.0);
		when(platilloRepository.findForAuthorizedCatalogSearch(any(), eq("%tacos%"), eq(null), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(ownedDish)));

		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "tacos", null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().items().get(0).ownedByNutritionist()).isTrue();
		assertThat(result.data().items().get(0).systemCatalog()).isFalse();
	}

	@Test
	void searchAppliesIngestasFilter() {
		when(platilloRepository.findForAuthorizedCatalogSearch(any(), eq("%avena%"), eq("%desayuno%"),
				any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "avena", "desayuno", 10);

		assertThat(result.success()).isTrue();
		verify(platilloRepository).findForAuthorizedCatalogSearch(any(), eq("%avena%"), eq("%desayuno%"),
				any(Pageable.class));
	}

	@Test
	void searchRejectsShortQuery() {
		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "a", null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchRejectsLimitAboveMaximum() {
		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "tacos", null, 30);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchRejectsBlankNutritionistId() {
		final AiToolResult<DishCatalogSearchData> result = service.search("  ", "tacos", null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchReturnsEmptyListWhenNoMatches() {
		when(platilloRepository.findForAuthorizedCatalogSearch(any(), eq("%xyzq%"), eq(null), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		final AiToolResult<DishCatalogSearchData> result = service.search(NUTRITIONIST_A, "xyzq", null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().items()).isEmpty();
		assertThat(result.data().totalReturned()).isZero();
	}

	private static Platillo samplePlatillo(final long id, final String name, final String userId,
			final String ingestasSugeridas, final int energia, final double proteina) {
		final Platillo platillo = new Platillo();
		platillo.setId(id);
		platillo.setName(name);
		platillo.setUserId(userId);
		platillo.setIngestasSugeridas(ingestasSugeridas);
		platillo.setEnergia(energia);
		platillo.setProteina(proteina);
		return platillo;
	}

}
