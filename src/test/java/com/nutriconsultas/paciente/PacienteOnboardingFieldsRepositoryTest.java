package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PacienteOnboardingFieldsRepositoryTest {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Test
	void newPaciente_defaultsStatusToActive() {
		final Paciente saved = pacienteRepository.save(legacyPaciente());

		assertThat(saved.getStatus()).isEqualTo(PacienteStatus.ACTIVE);
	}

	@Test
	void invitedPaciente_persistsOnboardingFields() {
		final Paciente paciente = invitedPaciente();
		final Paciente saved = pacienteRepository.saveAndFlush(paciente);

		final Paciente reloaded = pacienteRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(PacienteStatus.INVITED);
		assertThat(reloaded.getAssignedId()).isEqualTo("PAT-ONB-42");
		assertThat(reloaded.getEmailHint()).isEqualTo("hint@example.com");
		assertThat(reloaded.getDisplayName()).isEqualTo("Onboarding Display");
		assertThat(reloaded.getPatientAuthSub()).isNull();
	}

	private static Paciente legacyPaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Legacy Patient");
		paciente.setUserId("auth0|nutritionist-legacy");
		final LocalDate dob = LocalDate.now().minusYears(40);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		return paciente;
	}

	private static Paciente invitedPaciente() {
		final Paciente paciente = legacyPaciente();
		paciente.setStatus(PacienteStatus.INVITED);
		paciente.setAssignedId("PAT-ONB-42");
		paciente.setEmailHint("hint@example.com");
		paciente.setDisplayName("Onboarding Display");
		return paciente;
	}

}
