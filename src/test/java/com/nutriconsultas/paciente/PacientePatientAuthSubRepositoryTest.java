package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PacientePatientAuthSubRepositoryTest {

	private static final String PATIENT_SUB = "auth0|patient-repo-test";

	@Autowired
	private PacienteRepository pacienteRepository;

	@Test
	void findByPatientAuthSub_returnsLinkedPaciente() {
		final Paciente saved = pacienteRepository.save(samplePaciente(PATIENT_SUB));

		final Optional<Paciente> found = pacienteRepository.findByPatientAuthSub(PATIENT_SUB);

		assertThat(found).isPresent();
		assertThat(found.orElseThrow().getId()).isEqualTo(saved.getId());
	}

	@Test
	void findByPatientAuthSub_returnsEmptyWhenUnlinked() {
		assertThat(pacienteRepository.findByPatientAuthSub("auth0|missing")).isEmpty();
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Repo Test Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(25);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		return paciente;
	}

}
