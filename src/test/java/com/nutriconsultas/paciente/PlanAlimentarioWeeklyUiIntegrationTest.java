package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml" })
class PlanAlimentarioWeeklyUiIntegrationTest {

	private static final String OWNER_SUB = "auth0|plan-alimentario-weekly-owner";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private PacienteDietaWeekdayRepository pacienteDietaWeekdayRepository;

	@Autowired
	private DietaRepository dietaRepository;

	private Paciente paciente;

	private Dieta patientMondayDieta;

	private PacienteDieta assignment;

	@BeforeEach
	void seedWeeklyAssignment() {
		paciente = new Paciente();
		paciente.setName("Ana Bravo");
		paciente.setUserId(OWNER_SUB);
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente = pacienteRepository.saveAndFlush(paciente);

		patientMondayDieta = new Dieta();
		patientMondayDieta.setNombre("Dieta lunes paciente");
		patientMondayDieta.setUserId(OWNER_SUB);
		patientMondayDieta.setPacienteId(paciente.getId());
		patientMondayDieta.setEnergia(1800);
		patientMondayDieta = dietaRepository.saveAndFlush(patientMondayDieta);

		assignment = new PacienteDieta();
		assignment.setPaciente(paciente);
		assignment.setAssignmentType(PacienteDietaAssignmentType.WEEKLY);
		assignment.setStartDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
		assignment.setStatus(PacienteDietaStatus.ACTIVE);
		assignment = pacienteDietaRepository.saveAndFlush(assignment);

		final PacienteDietaWeekday monday = new PacienteDietaWeekday();
		monday.setPacienteDieta(assignment);
		monday.setDayOfWeek(1);
		monday.setDieta(patientMondayDieta);
		pacienteDietaWeekdayRepository.saveAndFlush(monday);
	}

	@Test
	void planAlimentario_rendersWeeklyDietLinksToPatientCopy() throws Exception {
		final MvcResult result = mockMvc
			.perform(get("/admin/pacientes/{id}/dietas", paciente.getId())
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Tester"))))
			.andExpect(status().isOk())
			.andReturn();

		final String html = result.getResponse().getContentAsString();
		assertThat(html).contains("Plan semanal");
		assertThat(html).contains("Dieta lunes paciente");
		assertThat(html).contains("/admin/dietas/" + patientMondayDieta.getId());
		assertThat(html).contains("Modificar dieta del paciente");
	}

}
