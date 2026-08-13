package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.appointmentquestion.AppointmentQuestionRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientAppointmentQuestionIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-appt-q-integration";

	private static final String OTHER_SUB = "auth0|mobile-appt-q-other";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private AppointmentQuestionRepository appointmentQuestionRepository;

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	private Paciente linkedPaciente;

	private Paciente otherPaciente;

	@BeforeEach
	void seedData() {
		rateLimiterRegistry.remove("patientAppointmentQuestions:" + LINKED_SUB);
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB)
			.orElseGet(() -> pacienteRepository.saveAndFlush(samplePaciente(LINKED_SUB, "Paciente preguntas")));
		otherPaciente = pacienteRepository.findByPatientAuthSub(OTHER_SUB)
			.orElseGet(() -> pacienteRepository.saveAndFlush(samplePaciente(OTHER_SUB, "Otro paciente preguntas")));
		appointmentQuestionRepository.findByPacienteId(linkedPaciente.getId())
			.forEach(appointmentQuestionRepository::delete);
		appointmentQuestionRepository.findByPacienteId(otherPaciente.getId())
			.forEach(appointmentQuestionRepository::delete);
		appointmentQuestionRepository.flush();
	}

	@Test
	void createAndListQuestionsWithLinkedJwt() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"¿Puedo comer mango?"}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.body").value("¿Puedo comer mango?"))
			.andExpect(jsonPath("$.data.answered").value(false))
			.andExpect(jsonPath("$.timestamp").exists());

		mockMvc.perform(get("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].body").value("¿Puedo comer mango?"));
	}

	@Test
	void getPatchAndDeleteQuestionRoundTrip() throws Exception {
		final Long questionId = createQuestion(LINKED_SUB, "¿Debo tomar más agua?");

		mockMvc.perform(get("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionId))
			.andExpect(jsonPath("$.data.body").value("¿Debo tomar más agua?"));

		mockMvc
			.perform(patch("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"answered":true}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.answered").value(true))
			.andExpect(jsonPath("$.data.answeredAt").exists());

		mockMvc
			.perform(get("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.param("answered", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0));

		mockMvc
			.perform(delete("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNoContent());

		assertThat(appointmentQuestionRepository.findByIdAndPacienteId(questionId, linkedPaciente.getId())).isEmpty();
	}

	@Test
	void otherPatientCannotAccessQuestionReturnsNotFound() throws Exception {
		final Long questionId = createQuestion(OTHER_SUB, "Pregunta ajena");

		mockMvc.perform(get("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());

		mockMvc
			.perform(patch("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"hack"}
						"""))
			.andExpect(status().isNotFound());

		mockMvc
			.perform(delete("/rest/mobile/patient/appointment-questions/{id}", questionId).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());

		assertThat(appointmentQuestionRepository.findByIdAndPacienteId(questionId, otherPaciente.getId())).isPresent();
	}

	@Test
	void createQuestionForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt("auth0|mobile-appt-q-unlinked"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"Pregunta"}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void createQuestionWithBlankBodyReturnsBadRequest() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"   "}
						"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void createQuestionRateLimitReturnsTooManyRequests() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"Primera"}
						"""))
			.andExpect(status().isCreated());
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"Segunda"}
						"""))
			.andExpect(status().isCreated());
		mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"Tercera"}
						"""))
			.andExpect(status().isTooManyRequests());
	}

	private Long createQuestion(final String sub, final String body) throws Exception {
		final MvcResult result = mockMvc
			.perform(post("/rest/mobile/patient/appointment-questions").with(mobileJwt(sub))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"body":"%s"}
						""".formatted(body)))
			.andExpect(status().isCreated())
			.andReturn();
		final JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		return root.path("data").path("id").asLong();
	}

	private static Paciente samplePaciente(final String patientAuthSub, final String name) {
		final Paciente paciente = new Paciente();
		paciente.setName(name);
		paciente.setUserId("auth0|nutritionist-owner");
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setDob(new java.util.Date());
		paciente.setGender("F");
		return paciente;
	}

}
