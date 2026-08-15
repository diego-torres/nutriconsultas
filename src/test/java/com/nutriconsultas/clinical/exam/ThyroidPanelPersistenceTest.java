package com.nutriconsultas.clinical.exam;

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

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@DataJpaTest
@ActiveProfiles("test")
class ThyroidPanelPersistenceTest {

	@Autowired
	private ClinicalExamRepository clinicalExamRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveAndLoad_persistsThyroidLibreColumnsMatchingLiquibase() {
		final Paciente paciente = pacienteRepository.saveAndFlush(samplePaciente());

		final ClinicalExam exam = new ClinicalExam();
		exam.setPaciente(paciente);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");
		exam.setTsh(2.5);
		exam.setT4Libre(1.2);
		exam.setT3Libre(3.1);
		exam.setAntiTpo(15.0);

		final ClinicalExam saved = clinicalExamRepository.saveAndFlush(exam);
		entityManager.clear();

		final ClinicalExam loaded = clinicalExamRepository.findById(saved.getId()).orElseThrow();

		assertThat(loaded.getTsh()).isCloseTo(2.5, within(0.001));
		assertThat(loaded.getT4Libre()).isCloseTo(1.2, within(0.001));
		assertThat(loaded.getT3Libre()).isCloseTo(3.1, within(0.001));
		assertThat(loaded.getAntiTpo()).isCloseTo(15.0, within(0.001));
	}

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Thyroid Persistence Test");
		paciente.setUserId("nutritionist-thyroid");
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		return paciente;
	}

}
