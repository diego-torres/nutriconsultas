package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.invitation.PatientInvitationTokenBundle;
import com.nutriconsultas.paciente.invitation.PatientInvitationTokenService;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.util.InvitationTokenHasher;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

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

	@Autowired
	private PatientInvitationTokenService patientInvitationTokenService;

	@Autowired
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	@MockBean
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@BeforeEach
	void resetRateLimiters() {
		rateLimiterRegistry.remove(PatientInvitationPreviewRateLimiter.PATIENT_INVITATION_PREVIEW + ":127.0.0.1");
	}

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

	@Test
	void previewInvitation_withoutJwt_returnsInviterDisplayName() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();
		seedPendingInvitation(bundle, NUTRITIONIST_SUB);
		seedNutritionistProfile(NUTRITIONIST_SUB, "Lic. Preview Nutri");

		mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", bundle.urlToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.inviterDisplayName").value("Lic. Preview Nutri"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void previewInvitation_withUnknownToken_returnsGenericNotFound() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();

		mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", bundle.urlToken()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("La invitación no es válida o ha expirado."));
	}

	@Test
	void previewInvitation_withExpiredToken_returnsSameMessageAsUnknown() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();
		final PatientInvitation invitation = seedPendingInvitation(bundle, NUTRITIONIST_SUB);
		invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
		patientInvitationRepository.saveAndFlush(invitation);

		mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", bundle.urlToken()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("La invitación no es válida o ha expirado."));
	}

	@Test
	void previewInvitation_withMalformedToken_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", "not-valid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("El enlace de invitación no es válido."));
	}

	@Test
	void previewInvitation_whenRateLimited_returns429WithRetryAfter() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();
		seedPendingInvitation(bundle, NUTRITIONIST_SUB);

		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", bundle.urlToken()))
				.andExpect(status().isOk());
		}

		mockMvc.perform(get("/rest/mobile/invitations/{token}/preview", bundle.urlToken()))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "60"))
			.andExpect(jsonPath("$.message").value("Demasiadas solicitudes. Inténtalo de nuevo en un minuto."));
	}

	private PatientInvitation seedPendingInvitation(final PatientInvitationTokenBundle bundle,
			final String nutritionistUserId) {
		final Paciente paciente = new Paciente();
		paciente.setUserId(nutritionistUserId);
		paciente.setName("Preview Patient");
		paciente.setEmail("preview." + UUID.randomUUID() + "@example.com");
		paciente.setStatus(PacienteStatus.INVITED);
		paciente.setGender("F");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		final Paciente savedPaciente = pacienteRepository.saveAndFlush(paciente);

		final PatientInvitation invitation = new PatientInvitation();
		invitation.setTokenHash(bundle.tokenHash());
		invitation.setPaciente(savedPaciente);
		invitation.setNutritionistUserId(nutritionistUserId);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
		invitation.setMaxUses(1);
		return patientInvitationRepository.saveAndFlush(invitation);
	}

	private void seedNutritionistProfile(final String nutritionistUserId, final String displayName) {
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setUserId(nutritionistUserId);
		profile.setPublicBookingId(UUID.randomUUID().toString());
		profile.setDisplayName(displayName);
		nutritionistProfileRepository.saveAndFlush(profile);
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
