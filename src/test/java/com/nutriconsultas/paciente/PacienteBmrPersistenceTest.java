package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Ensures BMR round-trips through JPA for {@link Paciente} (issue #57).
 */
@DataJpaTest
@ActiveProfiles("test")
class PacienteBmrPersistenceTest {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveAndReload_preservesBmrKcalPerDay() {
		final Paciente paciente = new Paciente();
		paciente.setName("BMR Round Trip");
		paciente.setUserId("user-bmr-test");
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		final Double expectedBmr = 1423.88;
		paciente.setBmr(expectedBmr);

		final Paciente saved = pacienteRepository.save(paciente);
		pacienteRepository.flush();
		entityManager.clear();

		final Paciente loaded = pacienteRepository.findById(saved.getId()).orElseThrow();

		assertThat(loaded.getBmr()).isCloseTo(expectedBmr, within(0.01));
	}

}
