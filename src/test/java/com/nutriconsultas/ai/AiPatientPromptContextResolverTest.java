package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.embeddable.PacienteBodySnapshot;
import com.nutriconsultas.paciente.satellite.PacienteMedicalHistory;

@ExtendWith(MockitoExtension.class)
class AiPatientPromptContextResolverTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiPatientPromptContextResolverImpl resolver;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void resolveBuildsRedactedContext() {
		final Paciente paciente = new Paciente();
		paciente.setId(42L);
		paciente.setGender("F");
		paciente.setPregnancy(false);
		paciente.setBodySnapshot(new PacienteBodySnapshot());
		paciente.getBodySnapshot().setImc(23.5);
		paciente.getBodySnapshot().setNivelPeso(NivelPeso.NORMAL);
		paciente.getBodySnapshot().setGetKcal(1800.0);
		paciente.getBodySnapshot().setFinalTotalKcal(2000.0);
		paciente.setPhysicalActivityLevel(PhysicalActivityLevel.MODERATE);
		paciente.setPhysiologicalStressActive(true);
		final PacienteMedicalHistory history = new PacienteMedicalHistory();
		history.setHipertension(true);
		history.setAlergias("Mariscos");
		paciente.setMedicalHistory(history);
		when(pacienteRepository.findByIdAndUserId(42L, NUTRITIONIST_ID)).thenReturn(Optional.of(paciente));

		final Optional<AiPatientPromptContext> context = resolver.resolve(42L, NUTRITIONIST_ID);

		assertThat(context).isPresent();
		assertThat(context.get().patientId()).isEqualTo(42L);
		assertThat(context.get().requerimientoKcal()).isEqualTo(1800.0);
		assertThat(context.get().alergias()).isEqualTo("Mariscos");
		assertThat(context.get().pathologyFlags()).containsEntry("hipertension", true);
	}

	@Test
	void resolveEmptyWhenPatientNotOwned() {
		when(pacienteRepository.findByIdAndUserId(99L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThat(resolver.resolve(99L, NUTRITIONIST_ID)).isEmpty();
	}

}
