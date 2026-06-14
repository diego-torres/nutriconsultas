package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricSource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientProgressIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-progress-integration";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private BodyMetricRecordRepository bodyMetricRecordRepository;

	private Paciente linkedPaciente;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		bodyMetricRecordRepository.findByPacienteIdOrderByRecordedAtAsc(linkedPaciente.getId())
			.forEach(bodyMetricRecordRepository::delete);
		linkedPaciente.setPeso(70.0);
		linkedPaciente.setEstatura(1.70);
		linkedPaciente.setImc(24.2);
		linkedPaciente.setNivelPeso(NivelPeso.NORMAL);
		linkedPaciente.setBmr(1500.0);
		pacienteRepository.saveAndFlush(linkedPaciente);

		saveMetric(linkedPaciente, 1L, 72.0, 24.9, Date.from(Instant.parse("2026-05-01T10:00:00Z")));
		saveMetric(linkedPaciente, 2L, 70.0, 24.2, Date.from(Instant.parse("2026-06-01T10:00:00Z")));
	}

	@Test
	void getProgressWithLinkedJwtReturnsSnapshot() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/progress").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.weightKg").value(70.0))
			.andExpect(jsonPath("$.data.bmi").value(closeTo(24.2, 0.01)))
			.andExpect(jsonPath("$.data.imcLabel").value("Normal"))
			.andExpect(jsonPath("$.data.bmr").value(1500.0))
			.andExpect(jsonPath("$.data.deltaPeso").value(-2.0))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void getProgressForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/progress").with(mobileJwt("auth0|mobile-progress-unlinked")))
			.andExpect(status().isForbidden());
	}

	@Test
	void listMeasurementsWithLinkedJwtReturnsAscendingSeries() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/progress/measurements").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.count").value(2))
			.andExpect(jsonPath("$.data.truncated").value(false))
			.andExpect(jsonPath("$.data.measurements[0].weightKg").value(72.0))
			.andExpect(jsonPath("$.data.measurements[1].weightKg").value(70.0))
			.andExpect(jsonPath("$.data.measurements[1].deltaPeso").doesNotExist())
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listMeasurementsForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc
			.perform(
					get("/rest/mobile/patient/progress/measurements").with(mobileJwt("auth0|mobile-progress-unlinked")))
			.andExpect(status().isForbidden());
	}

	@Test
	void listMeasurementsCapsAt365RowsForLargeHistory() throws Exception {
		for (int index = 3; index <= 402; index++) {
			saveMetric(linkedPaciente, (long) index, 70.0 + index, 24.0,
					Date.from(Instant.parse("2025-01-01T00:00:00Z").plusSeconds(index * 86_400L)));
		}

		mockMvc.perform(get("/rest/mobile/patient/progress/measurements").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.count").value(365))
			.andExpect(jsonPath("$.data.truncated").value(true));
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Paciente progreso");
		paciente.setUserId("auth0|nutritionist-owner");
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setDob(new Date());
		paciente.setGender("F");
		return paciente;
	}

	private void saveMetric(final Paciente paciente, final Long sourceId, final Double weight, final Double imc,
			final Date recordedAt) {
		final BodyMetricRecord record = new BodyMetricRecord();
		record.setPaciente(paciente);
		record.setSource(BodyMetricSource.ANTHROPOMETRIC);
		record.setSourceId(sourceId);
		record.setWeight(weight);
		record.setHeight(1.70);
		record.setImc(imc);
		record.setNivelPeso(NivelPeso.NORMAL);
		record.setRecordedAt(recordedAt);
		bodyMetricRecordRepository.saveAndFlush(record);
	}

}
