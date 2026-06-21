package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

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

	@Autowired
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	private Paciente linkedPaciente;

	@BeforeEach
	void seedData() {
		rateLimiterRegistry.remove(PatientWriteRateLimiter.PATIENT_MESSAGES + ":" + LINKED_SUB);
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		patientMessageRepository.findThreadForPatient(linkedPaciente.getId(), null, PageRequest.of(0, 500))
			.forEach(patientMessageRepository::delete);
		final PatientMessage message = new PatientMessage();
		message.setPaciente(linkedPaciente);
		message.setNutritionistUserId(linkedPaciente.getUserId());
		message.setSenderRole(MessageSenderRole.NUTRITIONIST);
		message.setBody("Bienvenido a tu consultorio digital");
		message.setSentAt(Instant.parse("2026-06-01T10:00:00Z"));
		message.setReadByPatient(false);
		message.setReadByNutritionist(true);
		patientMessageRepository.saveAndFlush(message);
		final NutritionistProfile profile = nutritionistProfileRepository.findByUserId(linkedPaciente.getUserId())
			.orElseGet(NutritionistProfile::new);
		profile.setUserId(linkedPaciente.getUserId());
		profile.setDisplayName("Lic. Nutri Minutriporcion");
		if (profile.getPublicBookingId() == null || profile.getPublicBookingId().isBlank()) {
			profile.setPublicBookingId(UUID.randomUUID().toString());
		}
		nutritionistProfileRepository.saveAndFlush(profile);
	}

	@Test
	void listMessagesWithLinkedJwtReturnsCursorPage() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].senderRole").value("NUTRITIONIST"))
			.andExpect(jsonPath("$.data.content[0].senderDisplayName").value("Lic. Nutri Minutriporcion"))
			.andExpect(jsonPath("$.data.content[0].body").value("Bienvenido a tu consultorio digital"))
			.andExpect(jsonPath("$.data.content[0].read").value(false))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listMessagesForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/messages").with(mobileJwt("auth0|mobile-message-unlinked")))
			.andExpect(status().isForbidden());
	}

	@Test
	void sendMessageWithLinkedJwtPersistsPatientMessage() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Tengo una duda sobre mi dieta\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.senderRole").value("PATIENT"))
			.andExpect(jsonPath("$.data.body").value("Tengo una duda sobre mi dieta"))
			.andExpect(jsonPath("$.data.read").value(true))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void sendMessageWithBlankBodyReturnsBadRequest() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("El texto del mensaje es obligatorio."))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void sendMessageForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt("auth0|mobile-message-unlinked"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Hola\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void sendMessageExceedingRateLimitReturns429WithLocalizedMessage() throws Exception {
		postMessage("Primer mensaje");
		postMessage("Segundo mensaje");

		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB))
				.header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Tercer mensaje\"}"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "60"))
			.andExpect(jsonPath("$.message").value("Too many requests. Please try again in a minute."))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	private void postMessage(final String body) throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"" + body + "\"}"))
			.andExpect(status().isCreated());
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
