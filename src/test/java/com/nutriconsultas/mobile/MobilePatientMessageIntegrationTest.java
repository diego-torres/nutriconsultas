package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientMessageIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-message-integration";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PatientMessageRepository patientMessageRepository;

	private Paciente linkedPaciente;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		patientMessageRepository
			.findThreadForPatient(linkedPaciente.getId(), null, org.springframework.data.domain.PageRequest.of(0, 1))
			.stream()
			.findFirst()
			.orElseGet(() -> {
				final PatientMessage message = new PatientMessage();
				message.setPaciente(linkedPaciente);
				message.setNutritionistUserId(linkedPaciente.getUserId());
				message.setSenderRole(MessageSenderRole.NUTRITIONIST);
				message.setBody("Bienvenido a tu consultorio digital");
				message.setSentAt(Instant.parse("2026-06-01T10:00:00Z"));
				message.setReadByPatient(false);
				message.setReadByNutritionist(true);
				return patientMessageRepository.saveAndFlush(message);
			});
	}

	@Test
	void listMessagesWithLinkedJwtReturnsCursorPage() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].senderRole").value("NUTRITIONIST"))
			.andExpect(jsonPath("$.data.content[0].body").value("Bienvenido a tu consultorio digital"))
			.andExpect(jsonPath("$.data.content[0].read").value(false))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listMessagesForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/messages").with(mobileJwt("auth0|mobile-message-unlinked")))
			.andExpect(status().isForbidden());
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Paciente mensajes");
		paciente.setUserId("auth0|nutritionist-owner");
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setDob(new java.util.Date());
		paciente.setGender("F");
		return paciente;
	}

}
