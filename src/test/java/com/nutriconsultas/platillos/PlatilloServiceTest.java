package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;

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

	@Test
	void duplicatePlatillo_copiesSystemPlatilloWithIngredients() {
		final Platillo original = systemPlatillo(1L);
		original.setDescription("Descripción");
		original.setIngestasSugeridas("Desayuno, Cena");
		original.setEnergia(300);
		original.setProteina(12.0);
		final Ingrediente ingrediente = new Ingrediente();
		ingrediente.setId(50L);
		ingrediente.setDescription("Frijol");
		ingrediente.setCantSugerida(1.0);
		ingrediente.setEnergia(100);
		final Alimento alimento = new Alimento();
		alimento.setId(7L);
		ingrediente.setAlimento(alimento);
		ingrediente.setUnidad("taza");
		original.setIngredientes(new ArrayList<>(List.of(ingrediente)));

		when(platilloRepository.findById(1L)).thenReturn(Optional.of(original));
		when(platilloRepository.save(any(Platillo.class))).thenAnswer(invocation -> {
			final Platillo saved = invocation.getArgument(0);
			saved.setId(99L);
			return saved;
		});

		final Platillo duplicated = platilloService.duplicatePlatillo(1L, TEST_USER_ID);

		assertThat(duplicated).isNotNull();
		assertThat(duplicated.getId()).isEqualTo(99L);
		assertThat(duplicated.getName()).isEqualTo("System platillo (copia)");
		assertThat(duplicated.getUserId()).isEqualTo(TEST_USER_ID);
		assertThat(duplicated.getDescription()).isEqualTo("Descripción");
		assertThat(duplicated.getIngestasSugeridas()).isEqualTo("Desayuno, Cena");
		assertThat(duplicated.getEnergia()).isEqualTo(300);
		assertThat(duplicated.getIngredientes()).hasSize(1);
		assertThat(duplicated.getIngredientes().get(0).getDescription()).isEqualTo("Frijol");
		assertThat(duplicated.getIngredientes().get(0).getPlatillo()).isSameAs(duplicated);
		assertThat(original.getName()).isEqualTo("System platillo");
		verify(platilloRepository).save(any(Platillo.class));
	}

	@Test
	void duplicatePlatillo_returnsNullWhenNotFound() {
		when(platilloRepository.findById(999L)).thenReturn(Optional.empty());

		assertThat(platilloService.duplicatePlatillo(999L, TEST_USER_ID)).isNull();
	}

	@Test
	void duplicatePlatillo_returnsNullForOtherNutritionistPlatillo() {
		when(platilloRepository.findById(5L)).thenReturn(Optional.of(ownedPlatillo(5L, "auth0|other")));

		assertThat(platilloService.duplicatePlatillo(5L, TEST_USER_ID)).isNull();
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
