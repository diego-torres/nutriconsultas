package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DietaServiceImplPickerTest {

	@InjectMocks
	private DietaServiceImpl dietaService;

	@Mock
	private DietaRepository dietaRepository;

	@Test
	void findPickerPageReturnsFirstPageAndHasNext() {
		final List<Dieta> dietas = new ArrayList<>();
		IntStream.rangeClosed(1, 25).forEach(i -> {
			final Dieta dieta = new Dieta();
			dieta.setId((long) i);
			dieta.setNombre("Dieta " + i);
			dietas.add(dieta);
		});
		when(dietaRepository.findAllCatalogDiets()).thenReturn(dietas);

		final DietaPickerPageDto page = dietaService.findPickerPage(null, 0, 20, null);

		assertThat(page.getItems()).hasSize(20);
		assertThat(page.getTotalElements()).isEqualTo(25);
		assertThat(page.isHasNext()).isTrue();
	}

	@Test
	void findPickerPageFiltersBySearchTerm() {
		final Dieta match = new Dieta();
		match.setId(1L);
		match.setNombre("Dieta 2000 kcal");
		final Dieta other = new Dieta();
		other.setId(2L);
		other.setNombre("Otra dieta");
		when(dietaRepository.findAllCatalogDiets()).thenReturn(List.of(match, other));

		final DietaPickerPageDto page = dietaService.findPickerPage("2000", 0, 20, null);

		assertThat(page.getItems()).hasSize(1);
		assertThat(page.getItems().get(0).getNombre()).isEqualTo("Dieta 2000 kcal");
	}

}
