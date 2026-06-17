package com.nutriconsultas.message;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientMessageIntegrationTest {

	private static final String NUTRITIONIST_SUB = "auth0|114-nutritionist";

	private static final String OTHER_NUTRITIONIST_SUB = "auth0|114-other-nutritionist";

	private static final String PATIENT_AUTH_SUB = "auth0|114-patient-linked";

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
		rateLimiterRegistry.remove("patientMessages:" + PATIENT_AUTH_SUB);
		linkedPaciente = pacienteRepository.findByPatientAuthSub(PATIENT_AUTH_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(PATIENT_AUTH_SUB, NUTRITIONIST_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		patientMessageRepository.findThreadForPatient(linkedPaciente.getId(), null, PageRequest.of(0, 500))
			.forEach(patientMessageRepository::delete);
		final NutritionistProfile profile = nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)
			.orElseGet(NutritionistProfile::new);
		profile.setUserId(NUTRITIONIST_SUB);
		profile.setDisplayName("Lic. Ana López");
		nutritionistProfileRepository.saveAndFlush(profile);
	}

	@Test
	void nutritionistReplyAppearsInMobileMessageThread() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/messages").with(mobileJwt(PATIENT_AUTH_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Tengo una duda sobre mi plan\"}"))
			.andExpect(status().isCreated());

		mockMvc
			.perform(post("/rest/patient-messages/thread/" + linkedPaciente.getId())
				.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_SUB).claim("name", "Nutritionist")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Claro, revisemos tu plan juntos\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.senderRole").value("NUTRITIONIST"))
			.andExpect(jsonPath("$.body").value("Claro, revisemos tu plan juntos"));

		mockMvc.perform(get("/rest/mobile/patient/messages").with(mobileJwt(PATIENT_AUTH_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].senderRole").value("NUTRITIONIST"))
			.andExpect(jsonPath("$.data.content[0].body").value("Claro, revisemos tu plan juntos"))
			.andExpect(jsonPath("$.data.content[0].senderDisplayName").value("Lic. Ana López"))
			.andExpect(jsonPath("$.data.content[0].read").value(false))
			.andExpect(jsonPath("$.data.content[1].senderRole").value("PATIENT"))
			.andExpect(jsonPath("$.data.content[1].body").value("Tengo una duda sobre mi plan"));
	}

	@Test
	void sendMessageForOtherNutritionistPatientReturnsNotFound() throws Exception {
		final Paciente otherPatient = pacienteRepository.findByPatientAuthSub("auth0|114-other-patient")
			.orElseGet(() -> pacienteRepository
				.saveAndFlush(samplePaciente("auth0|114-other-patient", OTHER_NUTRITIONIST_SUB)));

		mockMvc
			.perform(post("/rest/patient-messages/thread/" + otherPatient.getId())
				.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_SUB).claim("name", "Nutritionist")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Hola\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void sendMessageWithoutAuthenticationRedirectsToLogin() throws Exception {
		mockMvc
			.perform(post("/rest/patient-messages/thread/" + linkedPaciente.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Hola\"}"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));
	}

	private static Paciente samplePaciente(final String patientAuthSub, final String nutritionistUserId) {
		final Paciente paciente = new Paciente();
		paciente.setName("Paciente integración 114");
		paciente.setUserId(nutritionistUserId);
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setDob(new java.util.Date());
		paciente.setGender("F");
		return paciente;
	}

}
