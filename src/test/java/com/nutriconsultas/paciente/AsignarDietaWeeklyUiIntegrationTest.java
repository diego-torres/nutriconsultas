package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.nutriconsultas.dieta.Dieta;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AsignarDietaWeeklyUiIntegrationTest {

	private static final String OWNER_SUB = "auth0|weekly-ui-owner";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	private Paciente paciente;

	@BeforeEach
	void seedPatient() {
		paciente = new Paciente();
		paciente.setName("Ana Bravo");
		paciente.setUserId(OWNER_SUB);
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente = pacienteRepository.saveAndFlush(paciente);
	}

	@Test
	void asignarDietaPage_exposesWeeklyPickerAndSharedCatalog() throws Exception {
		final MvcResult result = mockMvc
			.perform(get("/admin/pacientes/{id}/dietas/asignar", paciente.getId())
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Tester"))))
			.andExpect(status().isOk())
			.andReturn();

		final String html = result.getResponse().getContentAsString();
		assertThat(html).contains("id=\"weeklySection\"");
		assertThat(html).contains("id=\"dietaPickerSection\"");
		assertThat(html).contains("id=\"weeklyPickerHint\"");
		assertThat(html).contains("id=\"dietaPickerList\"");
		assertThat(html).contains("Seleccionar dieta");
		assertThat(html).contains("No se requiere calcular GET/TEF");
		assertThat(html).contains("id=\"assignmentTypeEmptyPlan\"");
		assertThat(html).contains("id=\"emptyPlanSection\"");
		assertThat(html).contains("Plan alimentario vacío");
		assertThat(html).contains("name=\"emptyDietaNombre\"");
		assertThat(html.indexOf("id=\"dietaPickerSection\"")).isGreaterThan(html.indexOf("id=\"dateRangeSection\""));
	}

	@Test
	void assignEmptyPlan_createsPatientOwnedDietWithoutCatalogSelection() throws Exception {
		mockMvc
			.perform(post("/admin/pacientes/{id}/dietas/asignar", paciente.getId())
				.with(oidcLogin().idToken(token -> token.subject(OWNER_SUB).claim("name", "Tester")))
				.param("assignmentType", "EMPTY_PLAN")
				.param("startDate", "2026-08-14")
				.param("status", "ACTIVE")
				.param("emptyDietaNombre", "Plan personalizado Ana"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/pacientes/" + paciente.getId() + "/dietas"));

		final List<PacienteDieta> assignments = pacienteDietaRepository
			.findByPacienteIdOrderByStartDateDesc(paciente.getId());
		assertThat(assignments).hasSize(1);
		final PacienteDieta assignment = assignments.get(0);
		final Dieta assignedDieta = assignment.getDieta();
		assertThat(assignedDieta).isNotNull();
		assertThat(assignedDieta.getNombre()).isEqualTo("Plan personalizado Ana");
		assertThat(assignedDieta.getPacienteId()).isEqualTo(paciente.getId());
		assertThat(assignedDieta.getUserId()).isEqualTo(OWNER_SUB);
		assertThat(assignment.getAssignmentType()).isEqualTo(PacienteDietaAssignmentType.DATE_RANGE);
		assertThat(assignment.getStatus()).isEqualTo(PacienteDietaStatus.ACTIVE);
	}

}
