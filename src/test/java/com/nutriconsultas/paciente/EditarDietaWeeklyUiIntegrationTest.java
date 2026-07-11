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

	@BeforeEach
	void seedWeeklyAssignment() {
		paciente = new Paciente();
		paciente.setName("Ana Bravo");
		paciente.setUserId(OWNER_SUB);
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente = pacienteRepository.saveAndFlush(paciente);

		final Dieta dieta = new Dieta();
		dieta.setNombre("Dieta semanal prueba");
		dieta.setUserId(OWNER_SUB);
		dieta.setEnergia(1800);
		dieta.setProteina(90.0);
		dieta.setLipidos(50.0);
		dieta.setHidratosDeCarbono(200.0);
		final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

		assignment = new PacienteDieta();
		assignment.setPaciente(paciente);
		assignment.setAssignmentType(PacienteDietaAssignmentType.WEEKLY);
		assignment.setStartDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
		assignment.setStatus(PacienteDietaStatus.ACTIVE);
		assignment = pacienteDietaRepository.saveAndFlush(assignment);

		final PacienteDietaWeekday monday = new PacienteDietaWeekday();
		monday.setPacienteDieta(assignment);
		monday.setDayOfWeek(1);
		monday.setDieta(savedDieta);
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

}
