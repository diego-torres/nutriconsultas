package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.util.InvitationTokenHasher;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileInvitationIntegrationTest {

	private static final String NUTRITIONIST_SUB = "auth0|mobile-invitation-nutritionist";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PatientInvitationRepository patientInvitationRepository;

	@MockBean
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Test
	void createInvitation_withoutJwt_returnsUnauthorized() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/invitations").contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson("ana.unauth@example.com")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createInvitation_withNutritionistJwt_createsInvitedPatientAndPendingInvitation() throws Exception {
		final String email = "ana.invite." + UUID.randomUUID() + "@example.com";
		final MvcResult result = mockMvc
			.perform(post("/rest/mobile/invitations").with(mobileJwt(NUTRITIONIST_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.invitationId").isNumber())
			.andExpect(jsonPath("$.data.pacienteId").isNumber())
			.andExpect(jsonPath("$.data.inviteUrl").isString())
			.andExpect(jsonPath("$.data.humanCode").value(org.hamcrest.Matchers.startsWith("NUTRI-")))
			.andExpect(jsonPath("$.data.expiresAt").exists())
			.andReturn();

		final JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
		final long pacienteId = data.path("pacienteId").asLong();
		final long invitationId = data.path("invitationId").asLong();
		final String inviteUrl = data.path("inviteUrl").asText();
		final String humanCode = data.path("humanCode").asText();
		final String rawToken = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

		assertThat(inviteUrl).contains("/i/");
		assertThat(humanCode).matches("NUTRI-[0-9A-Z]{4}-[0-9A-Z]{4}");

		final var paciente = pacienteRepository.findById(pacienteId).orElseThrow();
		assertThat(paciente.getStatus()).isEqualTo(PacienteStatus.INVITED);
		assertThat(paciente.getUserId()).isEqualTo(NUTRITIONIST_SUB);
		assertThat(paciente.getPatientAuthSub()).isNull();
		assertThat(paciente.getAssignedId()).isNotBlank();

		final var invitation = patientInvitationRepository.findById(invitationId).orElseThrow();
		assertThat(invitation.getStatus()).isEqualTo(PatientInvitationStatus.PENDING);
		assertThat(invitation.getTokenHash()).hasSize(64);
		assertThat(InvitationTokenHasher.verifyToken(rawToken, invitation.getTokenHash())).isTrue();
	}

	@Test
	void createInvitation_doesNotRequirePatientLinkage() throws Exception {
		final String email = "solo.nutri." + UUID.randomUUID() + "@example.com";
		mockMvc
			.perform(post("/rest/mobile/invitations").with(mobileJwt("auth0|unlinked-nutritionist-only"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson(email)))
			.andExpect(status().isCreated());
	}

	private static String validRequestJson(final String email) {
		return """
				{
				  "name": "Ana Test",
				  "email": "%s",
				  "dob": "1992-03-10",
				  "gender": "F"
				}
				""".formatted(email);
	}

}
