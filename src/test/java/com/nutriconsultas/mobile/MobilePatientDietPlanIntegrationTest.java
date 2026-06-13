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

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientDietPlanIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-diet-plan-integration";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private DietaRepository dietaRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	private Paciente linkedPaciente;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		if (pacienteDietaRepository.findByPacienteId(linkedPaciente.getId()).isEmpty()) {
			final Dieta dieta = new Dieta();
			dieta.setNombre("Dieta integración");
			dieta.setUserId("nutritionist-sub");
			dieta.setEnergia(1900);
			dieta.setProteina(95.0);
			dieta.setLipidos(65.0);
			dieta.setHidratosDeCarbono(210.0);
			final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

			final PacienteDieta assignment = new PacienteDieta();
			assignment.setPaciente(linkedPaciente);
			assignment.setDieta(savedDieta);
			assignment.setStatus(PacienteDietaStatus.ACTIVE);
			assignment
				.setStartDate(Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()));
			assignment.setNotes("Plan activo de prueba");
			pacienteDietaRepository.saveAndFlush(assignment);
		}
	}

	@Test
	void listDietPlansWithLinkedJwtReturnsPagedSummaries() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].dietaName").value("Dieta integración"))
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.content[0].totalKcal").value(1900))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listDietPlansActiveOnlyFiltersResults() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans").param("activeOnly", "true").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Mobile Diet Plan Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
