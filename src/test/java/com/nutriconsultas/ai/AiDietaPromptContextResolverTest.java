package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class AiDietaPromptContextResolverTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiDietaPromptContextResolverImpl resolver;

	@Mock
	private DietaService dietaService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void resolveReturnsCatalogDietaContext() {
		final Dieta dieta = new Dieta();
		dieta.setId(8L);
		dieta.setNombre("1800 kcal");
		dieta.setUserId(NUTRITIONIST_ID);
		dieta.setEnergia(1800);
		dieta.setProteina(90.0);
		dieta.setLipidos(60.0);
		dieta.setHidratosDeCarbono(200.0);
		final Ingesta ingesta = new Ingesta();
		ingesta.setNombre("Desayuno");
		dieta.setIngestas(List.of(ingesta));
		when(dietaService.getDieta(8L)).thenReturn(dieta);

		final Optional<AiDietaPromptContext> context = resolver.resolve(8L, NUTRITIONIST_ID);

		assertThat(context).isPresent();
		assertThat(context.get().dietaId()).isEqualTo(8L);
		assertThat(context.get().nombre()).isEqualTo("1800 kcal");
		assertThat(context.get().ingestaNames()).containsExactly("Desayuno");
	}

	@Test
	void resolveRejectsForeignPatientDieta() {
		final Dieta dieta = new Dieta();
		dieta.setId(9L);
		dieta.setPacienteId(20L);
		when(dietaService.getDieta(9L)).thenReturn(dieta);
		when(pacienteRepository.findByIdAndUserId(20L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> resolver.resolve(9L, NUTRITIONIST_ID)).isInstanceOf(AiChatException.class)
			.extracting(ex -> ((AiChatException) ex).getHttpStatus())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

}
