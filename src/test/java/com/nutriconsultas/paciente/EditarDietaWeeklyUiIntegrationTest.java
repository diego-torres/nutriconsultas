package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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
class EditarDietaWeeklyUiIntegrationTest {

	private static final String OWNER_SUB = "auth0|weekly-edit-owner";

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

	private PacienteDieta assignment;

	private Dieta weekdayDieta;

	@BeforeEach
	void seedWeeklyAssignment() {
		paciente = new Paciente();
		paciente.setName("Ana Bravo");
		paciente.setUserId(OWNER_SUB);
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente = pacienteRepository.saveAndFlush(paciente);

		weekdayDieta = new Dieta();
		weekdayDieta.setNombre("Dieta semanal prueba");
		weekdayDieta.setUserId(OWNER_SUB);
		weekdayDieta.setPacienteId(paciente.getId());
		weekdayDieta.setEnergia(1800);
		weekdayDieta.setProteina(90.0);
		weekdayDieta.setLipidos(50.0);
		weekdayDieta.setHidratosDeCarbono(200.0);
		weekdayDieta = dietaRepository.saveAndFlush(weekdayDieta);

		assignment = new PacienteDieta();
		assignment.setPaciente(paciente);
		assignment.setAssignmentType(PacienteDietaAssignmentType.WEEKLY);
		assignment.setStartDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
		assignment.setStatus(PacienteDietaStatus.ACTIVE);
		assignment = pacienteDietaRepository.saveAndFlush(assignment);

		final PacienteDietaWeekday monday = new PacienteDietaWeekday();
		monday.setPacienteDieta(assignment);
		monday.setDayOfWeek(1);
		monday.setDieta(weekdayDieta);
		pacienteDietaWeekdayRepository.saveAndFlush(monday);
	}

	@Test
	void editarDietaPage_rendersWeeklyDayLabels() throws Exception {
		final MvcResult result = mockMvc
			.perform(get("/admin/pacientes/{pacienteId}/dietas/{id}/editar", paciente.getId(), assignment.getId())
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Tester"))))
			.andExpect(status().isOk())
			.andReturn();

		final String html = result.getResponse().getContentAsString();
		assertThat(html).contains("Plan semanal");
		assertThat(html).contains("Menú por día");
		assertThat(html).contains("Dieta semanal prueba");
	}

	@Test
	void editarDieta_saveResubmitsExistingPatientCopyWithoutServerError() throws Exception {
		mockMvc
			.perform(post("/admin/pacientes/{pacienteId}/dietas/{id}/editar", paciente.getId(), assignment.getId())
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Tester")))
				.param("startDate", LocalDate.now().toString())
				.param("status", "ACTIVE")
				.param("weekdayDietaId_1", String.valueOf(weekdayDieta.getId())))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/pacientes/" + paciente.getId() + "/dietas"));

		final List<PacienteDietaWeekday> slots = pacienteDietaWeekdayRepository
			.findByPacienteDietaIdOrderByDayOfWeekAsc(assignment.getId());
		assertThat(slots).hasSize(1);
		assertThat(slots.get(0).getDieta().getId()).isEqualTo(weekdayDieta.getId());
		assertThat(slots.get(0).getDayOfWeek()).isEqualTo(1);
	}

}
