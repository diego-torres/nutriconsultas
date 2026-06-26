package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientInvitationLandingControllerTest {

	private static final String NUTRITIONIST_SUB = "auth0|landing-controller-nutritionist";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PatientInvitationTokenService patientInvitationTokenService;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PatientInvitationRepository patientInvitationRepository;

	@Autowired
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	@MockBean
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@BeforeEach
	void resetRateLimiters() {
		rateLimiterRegistry
			.remove(com.nutriconsultas.mobile.PatientInvitationPreviewRateLimiter.PATIENT_INVITATION_PREVIEW
					+ ":127.0.0.1");
	}

	@Test
	void landingPage_withValidToken_rendersLandingView() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();
		seedPendingInvitation(bundle);

		final MvcResult result = mockMvc.perform(get("/links/i/{token}", bundle.urlToken()))
			.andExpect(status().isOk())
			.andExpect(view().name("eterna/patient-invitation-landing"))
			.andReturn();

		final String html = result.getResponse().getContentAsString();
		assertThat(html).contains(bundle.humanCode());
		assertThat(html).contains("Dra. Web Landing");
		assertThat(html).contains("Tu plan en tu bolsillo");
		assertThat(html).contains("Monitorea tu progreso");
		assertThat(html).contains("Mantente en contacto");
	}

	@Test
	void legacyLandingPath_withValidToken_rendersLandingView() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();
		seedPendingInvitation(bundle);

		mockMvc.perform(get("/i/{token}", bundle.urlToken()))
			.andExpect(status().isOk())
			.andExpect(view().name("eterna/patient-invitation-landing"));
	}

	@Test
	void landingPage_withUnknownToken_rendersUnavailableMessage() throws Exception {
		final PatientInvitationTokenBundle bundle = patientInvitationTokenService.generate();

		final MvcResult result = mockMvc.perform(get("/links/i/{token}", bundle.urlToken()))
			.andExpect(status().isOk())
			.andExpect(view().name("eterna/patient-invitation-landing"))
			.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("Este enlace ya no está disponible");
	}

	private void seedPendingInvitation(final PatientInvitationTokenBundle bundle) {
		final Paciente paciente = new Paciente();
		paciente.setAssignedId("LAND-" + UUID.randomUUID());
		paciente.setName("Paciente Landing");
		paciente.setEmail("landing." + UUID.randomUUID() + "@example.com");
		paciente.setDob(Date.from(LocalDate.of(1990, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		paciente.setUserId(NUTRITIONIST_SUB);
		paciente.setStatus(PacienteStatus.INVITED);
		pacienteRepository.save(paciente);

		final PatientInvitation invitation = new PatientInvitation();
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(NUTRITIONIST_SUB);
		invitation.setTokenHash(bundle.tokenHash());
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
		patientInvitationRepository.save(invitation);

		if (nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB).isEmpty()) {
			final NutritionistProfile profile = new NutritionistProfile();
			profile.setUserId(NUTRITIONIST_SUB);
			profile.setPublicBookingId(UUID.randomUUID().toString());
			profile.setDisplayName("Dra. Web Landing");
			nutritionistProfileRepository.save(profile);
		}
	}

}
