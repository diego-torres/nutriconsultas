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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

@ExtendWith(MockitoExtension.class)
class SearchFoodCatalogToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private SearchFoodCatalogToolServiceImpl service;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Test
	void searchReturnsMappedItemsWithDefaultLimit() {
		final Alimento avena = sampleAlimento(1L, "Avena", "Cereales", "taza", 0.5, 150);
		when(alimentosRepository.countForCatalogSearch("%avena%", null)).thenReturn(1L);
		when(alimentosRepository.findForCatalogSearch(eq("%avena%"), eq(null), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(avena)));

		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "avena", null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().items()).hasSize(1);
		assertThat(result.data().items().get(0).alimentoId()).isEqualTo(1L);
		assertThat(result.data().items().get(0).nombreAlimento()).isEqualTo("Avena");
		assertThat(result.data().items().get(0).clasificacion()).isEqualTo("Cereales");
		assertThat(result.data().items().get(0).unidad()).isEqualTo("taza");
		assertThat(result.data().items().get(0).cantSugerida()).isEqualTo(0.5);
		assertThat(result.data().items().get(0).energiaKcalPorPorcion()).isEqualTo(150);
		assertThat(result.data().totalReturned()).isEqualTo(1);
		assertThat(result.data().truncated()).isFalse();

		final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(alimentosRepository).findForCatalogSearch(eq("%avena%"), eq(null), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(SearchFoodCatalogToolServiceImpl.DEFAULT_LIMIT);
	}

	@Test
	void searchAppliesClasificacionFilterAndCustomLimit() {
		when(alimentosRepository.countForCatalogSearch("%leche%", "%lácteos%")).thenReturn(1L);
		when(alimentosRepository.findForCatalogSearch(eq("%leche%"), eq("%lácteos%"), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(sampleAlimento(2L, "Leche", "Lácteos", "taza", 1.0, 120))));

		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "leche", "lácteos", 5);

		assertThat(result.success()).isTrue();
		assertThat(result.data().truncated()).isFalse();
		verify(alimentosRepository).findForCatalogSearch(eq("%leche%"), eq("%lácteos%"), any(Pageable.class));
	}

	@Test
	void searchMarksTruncatedWhenMoreMatchesExist() {
		when(alimentosRepository.countForCatalogSearch("%arroz%", null)).thenReturn(40L);
		when(alimentosRepository.findForCatalogSearch(eq("%arroz%"), eq(null), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(sampleAlimento(3L, "Arroz", "Cereales", "taza", 1.0, 200))));

		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "arroz", null, 10);

		assertThat(result.success()).isTrue();
		assertThat(result.data().totalReturned()).isEqualTo(1);
		assertThat(result.data().truncated()).isTrue();
	}

	@Test
	void searchRejectsShortQuery() {
		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "a", null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchRejectsOversizedQuery() {
		final String longQuery = "x".repeat(SearchFoodCatalogToolServiceImpl.MAX_QUERY_LENGTH + 1);

		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, longQuery, null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchRejectsLimitAboveMaximum() {
		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "avena", null, 30);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchRejectsBlankNutritionistId() {
		final AiToolResult<FoodCatalogSearchData> result = service.search("  ", "avena", null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void searchReturnsEmptyListWhenNoMatches() {
		when(alimentosRepository.countForCatalogSearch("%xyzq%", null)).thenReturn(0L);
		when(alimentosRepository.findForCatalogSearch(eq("%xyzq%"), eq(null), any(Pageable.class)))
			.thenReturn(Page.empty());

		final AiToolResult<FoodCatalogSearchData> result = service.search(NUTRITIONIST_ID, "xyzq", null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().items()).isEmpty();
		assertThat(result.data().totalReturned()).isZero();
		assertThat(result.data().truncated()).isFalse();
	}

	private static Alimento sampleAlimento(final long id, final String nombre, final String clasificacion,
			final String unidad, final double cantSugerida, final int energia) {
		final Alimento alimento = new Alimento();
		alimento.setId(id);
		alimento.setNombreAlimento(nombre);
		alimento.setClasificacion(clasificacion);
		alimento.setUnidad(unidad);
		alimento.setCantSugerida(cantSugerida);
		alimento.setEnergia(energia);
		return alimento;
	}

}
