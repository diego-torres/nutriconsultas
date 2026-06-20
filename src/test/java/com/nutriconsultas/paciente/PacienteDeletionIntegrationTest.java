package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PacienteDeletionIntegrationTest {

	private static final String OWNER_SUB = "auth0|223-owner";

	private static final String OTHER_SUB = "auth0|223-other";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	@Autowired
	private ClinicalExamRepository clinicalExamRepository;

	@Autowired
	private PatientMessageRepository patientMessageRepository;

	@Autowired
	private BodyMetricRecordRepository bodyMetricRecordRepository;

	private Paciente ownedPaciente;

	@BeforeEach
	void seedPatient() {
		ownedPaciente = pacienteRepository.save(samplePaciente(OWNER_SUB));
		final CalendarEvent event = new CalendarEvent();
		event.setPaciente(ownedPaciente);
		event.setEventDateTime(new Date());
		event.setTitle("Consulta");
		event.setDurationMinutes(60);
		event.setStatus(EventStatus.SCHEDULED);
		calendarEventRepository.saveAndFlush(event);

		final ClinicalExam exam = new ClinicalExam();
		exam.setPaciente(ownedPaciente);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen");
		clinicalExamRepository.saveAndFlush(exam);

		final PatientMessage message = new PatientMessage();
		message.setPaciente(ownedPaciente);
		message.setNutritionistUserId(OWNER_SUB);
		message.setSenderRole(MessageSenderRole.PATIENT);
		message.setBody("Hola");
		message.setSentAt(Instant.now());
		patientMessageRepository.saveAndFlush(message);
	}

	@Test
	void deletePaciente_removesPatientAndHistoryForOwner() throws Exception {
		final Long pacienteId = ownedPaciente.getId();

		mockMvc
			.perform(delete("/rest/pacientes/" + pacienteId)
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Owner"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		assertThat(pacienteRepository.findById(pacienteId)).isEmpty();
		assertThat(calendarEventRepository.findByPacienteId(pacienteId)).isEmpty();
		assertThat(clinicalExamRepository.findByPacienteId(pacienteId)).isEmpty();
		assertThat(patientMessageRepository.findThreadForPatient(pacienteId, null,
				org.springframework.data.domain.PageRequest.of(0, 10)))
			.isEmpty();
		assertThat(bodyMetricRecordRepository.findByPacienteIdOrderByRecordedAtAsc(pacienteId)).isEmpty();
	}

	@Test
	void deletePaciente_returnsNotFoundForOtherTenant() throws Exception {
		final Long pacienteId = ownedPaciente.getId();

		mockMvc
			.perform(delete("/rest/pacientes/" + pacienteId)
				.with(oidcLogin().idToken(token -> token.subject(OTHER_SUB).claim("name", "Other"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false));

		assertThat(pacienteRepository.findById(pacienteId)).isPresent();
	}

	private static Paciente samplePaciente(final String userId) {
		final Paciente paciente = new Paciente();
		paciente.setUserId(userId);
		paciente.setName("Deletion Test Patient");
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 5, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setEmail("delete-test@example.com");
		paciente.setPatientAuthSub("auth0|delete-test-patient");
		return paciente;
	}

}
