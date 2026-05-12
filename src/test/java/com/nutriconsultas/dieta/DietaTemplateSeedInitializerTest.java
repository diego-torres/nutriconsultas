package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloRepository;

@ExtendWith(MockitoExtension.class)
class DietaTemplateSeedInitializerTest {

	@Mock
	private DietaRepository dietaRepository;

	@Mock
	private PlatilloRepository platilloRepository;

	@InjectMocks
	private DietaTemplateSeedInitializer initializer;

	@Test
	void runSkipsWhenNoPlatillos() {
		when(platilloRepository.count()).thenReturn(0L);
		initializer.run();
		verifyNoInteractions(dietaRepository);
	}

	@Test
	void runSkipsWhenTemplatesAlreadyExist() {
		when(platilloRepository.count()).thenReturn(5L);
		final Dieta existing = new Dieta();
		when(dietaRepository.findByUserId(DietaTemplateSeedInitializer.TEMPLATE_DIETA_USER_ID))
			.thenReturn(java.util.List.of(existing));
		initializer.run();
		verify(dietaRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void runFailsFastWhenPlatilloNameMissing() {
		when(platilloRepository.count()).thenReturn(5L);
		when(dietaRepository.findByUserId(DietaTemplateSeedInitializer.TEMPLATE_DIETA_USER_ID))
			.thenReturn(java.util.List.of());
		when(platilloRepository.findFirstByNameIgnoreCaseOrderByIdAsc(anyString()))
			.thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> initializer.run()).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("missing platillo");
	}

	@Test
	void runSeedsWhenPlatillosResolve() {
		when(platilloRepository.count()).thenReturn(5L);
		when(dietaRepository.findByUserId(DietaTemplateSeedInitializer.TEMPLATE_DIETA_USER_ID))
			.thenReturn(java.util.List.of());
		final Platillo platillo = new Platillo();
		platillo.setName("Huevos revueltos con tortilla");
		final Ingrediente ing = new Ingrediente();
		platillo.getIngredientes().add(ing);
		when(platilloRepository.findFirstByNameIgnoreCaseOrderByIdAsc(anyString()))
			.thenReturn(java.util.Optional.of(platillo));

		initializer.run();

		verify(dietaRepository, times(20)).save(org.mockito.ArgumentMatchers.any());
	}

}
