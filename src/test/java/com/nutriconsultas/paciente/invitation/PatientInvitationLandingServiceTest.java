package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.mobile.PatientInvitationPreviewRateLimiter;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

@ExtendWith(MockitoExtension.class)
class PatientInvitationLandingServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|landing-nutritionist";

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Mock
	private PatientInvitationPreviewRateLimiter patientInvitationPreviewRateLimiter;

	private PatientInvitationProperties invitationProperties;

	private PatientInvitationTokenService tokenService;

	private PatientInvitationLandingService service;

	@BeforeEach
	void setUp() {
		invitationProperties = new PatientInvitationProperties();
		invitationProperties.setBaseUrl("https://minutriporcion.com");
		tokenService = new PatientInvitationTokenServiceImpl(invitationProperties);
		final PatientInvitationPreviewService previewService = new PatientInvitationPreviewServiceImpl(
				patientInvitationRepository, nutritionistProfileRepository);
		service = new PatientInvitationLandingServiceImpl(previewService, patientInvitationPreviewRateLimiter,
				invitationProperties);
		when(patientInvitationPreviewRateLimiter.execute(any(), any())).thenAnswer(invocation -> {
			final java.util.concurrent.Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});
	}

	@Test
	void resolve_withValidPendingInvitation_returnsLandingContent() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Dra. Landing");
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.of(profile));

		final Optional<PatientInvitationLandingContent> content = service.resolve(bundle.urlToken(), "127.0.0.1");

		assertThat(content).isPresent();
		assertThat(content.orElseThrow().inviterDisplayName()).isEqualTo("Dra. Landing");
		assertThat(content.orElseThrow().humanCode()).isEqualTo(bundle.humanCode());
		assertThat(content.orElseThrow().inviteUrl())
			.isEqualTo("https://minutriporcion.com/links/i/" + bundle.urlToken());
	}

	@Test
	void resolve_withUnknownToken_returnsEmpty() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.empty());

		assertThat(service.resolve(bundle.urlToken(), "127.0.0.1")).isEmpty();
	}

	@Test
	void resolve_withMalformedToken_returnsEmpty() {
		assertThat(service.resolve("not-a-token", "127.0.0.1")).isEmpty();
	}

	private static PatientInvitation pendingInvitation(final String tokenHash) {
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setId(10L);
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(NUTRITIONIST_SUB);
		invitation.setTokenHash(tokenHash);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		return invitation;
	}

}
