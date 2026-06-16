package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;

/**
 * Ensures body snapshot embeddable and Phase C satellite rows round-trip through JPA —
 * #156.
 */
@DataJpaTest
@ActiveProfiles("test")
class PacienteEmbeddablePersistenceTest {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveAndLoad_preservesEmbeddableFieldsViaDelegatedAccessors() {
		final Paciente paciente = samplePaciente();
		paciente.setPeso(72.5);
		paciente.setEstatura(1.70);
		paciente.setImc(25.1);
		paciente.setBmr(1500.0);
		paciente.setGetKcal(2100.0);
		paciente.setActivityFactorScale(ActivityFactorScale.HARRIS_BENEDICT);
		paciente.setPreferredBmrFormula(BmrFormulaType.PROMEDIO);
		paciente.setPhysicalActivityLevel(PhysicalActivityLevel.MODERATE);
		paciente.setAntecedentesPrenatales("Sin antecedentes");
		paciente.setHipertension(true);
		paciente.setDiabetes(false);

		final Paciente saved = pacienteRepository.saveAndFlush(paciente);
		entityManager.clear();

		final Paciente loaded = pacienteRepository.findById(saved.getId()).orElseThrow();

		assertThat(loaded.getPeso()).isEqualTo(72.5);
		assertThat(loaded.getEstatura()).isEqualTo(1.70);
		assertThat(loaded.getImc()).isEqualTo(25.1);
		assertThat(loaded.getBmr()).isEqualTo(1500.0);
		assertThat(loaded.getGetKcal()).isEqualTo(2100.0);
		assertThat(loaded.getActivityFactorScale()).isEqualTo(ActivityFactorScale.HARRIS_BENEDICT);
		assertThat(loaded.getPreferredBmrFormula()).isEqualTo(BmrFormulaType.PROMEDIO);
		assertThat(loaded.getPhysicalActivityLevel()).isEqualTo(PhysicalActivityLevel.MODERATE);
		assertThat(loaded.getAntecedentesPrenatales()).isEqualTo("Sin antecedentes");
		assertThat(loaded.getHipertension()).isTrue();
		assertThat(loaded.getDiabetes()).isFalse();
	}

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Embeddable Test");
		paciente.setUserId("nutritionist-embeddable");
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
