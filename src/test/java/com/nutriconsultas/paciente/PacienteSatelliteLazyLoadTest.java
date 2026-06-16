package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.projection.PacienteListView;

/**
 * Verifies #156 Phase C: patient grid projections do not join satellite tables (no N+1 on
 * list paths).
 */
@DataJpaTest
@ActiveProfiles("test")
class PacienteSatelliteLazyLoadTest {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private SessionFactory sessionFactory;

	private Statistics statistics;

	@BeforeEach
	void enableStatistics() {
		statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
	}

	@Test
	void listProjection_usesSingleQueryWithoutSatelliteJoins() {
		final Paciente paciente = samplePaciente();
		paciente.setAntecedentesPrenatales("Projection isolation");
		paciente.setHipertension(true);
		pacienteRepository.saveAndFlush(paciente);
		entityManager.clear();
		statistics.clear();

		final List<PacienteListView> views = pacienteRepository
			.findListViewsByUserId("nutritionist-satellite", PageRequest.of(0, 10))
			.getContent();

		assertThat(views).hasSize(1);
		assertThat(views.get(0).getName()).isEqualTo("Satellite Lazy Test");
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	@Test
	void findById_loadsSatelliteDataViaDelegatedAccessors() {
		final Paciente paciente = samplePaciente();
		paciente.setAntecedentesPrenatales("Sin antecedentes");
		paciente.setHipertension(true);

		final Paciente saved = pacienteRepository.saveAndFlush(paciente);
		entityManager.clear();

		final Paciente loaded = pacienteRepository.findById(saved.getId()).orElseThrow();

		assertThat(loaded.getAntecedentesPrenatales()).isEqualTo("Sin antecedentes");
		assertThat(loaded.getHipertension()).isTrue();
	}

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Satellite Lazy Test");
		paciente.setUserId("nutritionist-satellite");
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
