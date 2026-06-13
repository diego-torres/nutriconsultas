package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientVisitIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-visit-integration";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	private Paciente linkedPaciente;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		if (calendarEventRepository.findByPacienteId(linkedPaciente.getId()).isEmpty()) {
			final CalendarEvent event = new CalendarEvent();
			event.setPaciente(linkedPaciente);
			event.setTitle("Consulta integración");
			event.setStatus(EventStatus.SCHEDULED);
			event.setDurationMinutes(60);
			event.setEventDateTime(
					Date.from(LocalDate.now().plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
			calendarEventRepository.saveAndFlush(event);
		}
	}

	@Test
	void listVisitsWithLinkedJwtReturnsPagedSummaries() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].title").value("Consulta integración"))
			.andExpect(jsonPath("$.data.content[0].status").value("SCHEDULED"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Mobile Visit Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
