package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.invitation.InvitationAuthPath;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.util.InvitationTokenHasher;

@ExtendWith(MockitoExtension.class)
class PatientInvitationPreviewServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|preview-nutritionist";

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	private PatientInvitationTokenService tokenService;

	private PatientInvitationPreviewService service;

	@BeforeEach
	void setUp() {
		tokenService = new PatientInvitationTokenServiceImpl(new PatientInvitationProperties());
		service = new PatientInvitationPreviewServiceImpl(patientInvitationRepository, nutritionistProfileRepository,
				new PatientInvitationProperties());
	}

	@Test
	void preview_withValidPendingInvitation_returnsInviterDisplayName() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Lic. Ana López");
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.of(profile));

		final PatientInvitationPreviewResult result = service.preview(bundle.urlToken());

		assertThat(result.inviterDisplayName()).isEqualTo("Lic. Ana López");
		assertThat(result.patientStatus()).isEqualTo(PacienteStatus.INVITED);
		assertThat(result.mobileAppLinked()).isFalse();
		assertThat(result.authPath()).isEqualTo(InvitationAuthPath.CREATE_ACCOUNT);
		assertThat(result.emailHint()).isNull();
	}

	@Test
	void preview_withInvitedUnlinkedPatient_returnsCreateAccountAuthPath() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.getPaciente().setEmail("patient@example.com");
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.empty());

		final PatientInvitationPreviewResult result = service.preview(bundle.urlToken());

		assertThat(result.authPath()).isEqualTo(InvitationAuthPath.CREATE_ACCOUNT);
		assertThat(result.mobileAppLinked()).isFalse();
		assertThat(result.emailHint()).isEqualTo("p***@example.com");
	}

	@Test
	void preview_withOnboardingLinkedPatient_returnsSignInAuthPath() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		final Paciente paciente = invitation.getPaciente();
		paciente.setStatus(PacienteStatus.ONBOARDING);
		paciente.setPatientAuthSub("auth0|linked-patient");
		paciente.setEmail("linked@example.com");
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.empty());

		final PatientInvitationPreviewResult result = service.preview(bundle.urlToken());

		assertThat(result.patientStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		assertThat(result.mobileAppLinked()).isTrue();
		assertThat(result.authPath()).isEqualTo(InvitationAuthPath.SIGN_IN);
		assertThat(result.emailHint()).isEqualTo("l***@example.com");
	}

	@Test
	void previewByHumanCode_withInvitedUnlinkedPatient_returnsCreateAccountAuthPath() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.setHumanCode(bundle.humanCode());
		invitation.getPaciente().setEmailHint("hint@example.com");
		when(patientInvitationRepository.findByHumanCode(bundle.humanCode())).thenReturn(Optional.of(invitation));
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.empty());

		final PatientInvitationPreviewResult result = service.previewByHumanCode(bundle.humanCode());

		assertThat(result.authPath()).isEqualTo(InvitationAuthPath.CREATE_ACCOUNT);
		assertThat(result.emailHint()).isEqualTo("h***@example.com");
	}

	@Test
	void preview_withUnknownTokenHash_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.preview(bundle.urlToken()))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void preview_withExpiredInvitation_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.preview(bundle.urlToken()))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void preview_withRevokedInvitation_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.setStatus(PatientInvitationStatus.REVOKED);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.preview(bundle.urlToken()))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void preview_withMalformedToken_throwsInvalid() {
		assertThatThrownBy(() -> service.preview("not-a-valid-token"))
			.isInstanceOf(PatientInvitationInvalidTokenException.class);
	}

	@Test
	void preview_absentAndExpiredTokens_useSameUnavailableException() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.empty());
		final PatientInvitation expired = pendingInvitation(
				InvitationTokenHasher.hashToken(tokenService.generate().urlToken()));
		expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
		final String expiredToken = tokenService.generate().urlToken();
		when(patientInvitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(expiredToken)))
			.thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> service.preview(bundle.urlToken()))
			.isExactlyInstanceOf(PatientInvitationUnavailableException.class);
		assertThatThrownBy(() -> service.preview(expiredToken))
			.isExactlyInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void previewByHumanCode_withValidPendingInvitation_returnsInviterDisplayName() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.setHumanCode(bundle.humanCode());
		when(patientInvitationRepository.findByHumanCode(bundle.humanCode())).thenReturn(Optional.of(invitation));
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Lic. Ana López");
		when(nutritionistProfileRepository.findByUserId(NUTRITIONIST_SUB)).thenReturn(Optional.of(profile));

		final PatientInvitationPreviewResult result = service.previewByHumanCode(bundle.humanCode());

		assertThat(result.inviterDisplayName()).isEqualTo("Lic. Ana López");
	}

	@Test
	void previewByHumanCode_withUnknownCode_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		when(patientInvitationRepository.findByHumanCode(bundle.humanCode())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.previewByHumanCode(bundle.humanCode()))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void previewByHumanCode_withMalformedCode_throwsUnavailable() {
		assertThatThrownBy(() -> service.previewByHumanCode("not-a-code"))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void previewByHumanCode_withExpiredInvitation_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash());
		invitation.setHumanCode(bundle.humanCode());
		invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
		when(patientInvitationRepository.findByHumanCode(bundle.humanCode())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.previewByHumanCode(bundle.humanCode()))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	private PatientInvitation pendingInvitation(final String tokenHash) {
		final Paciente paciente = new Paciente();
		paciente.setId(100L);
		paciente.setStatus(PacienteStatus.INVITED);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setId(1L);
		invitation.setTokenHash(tokenHash);
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(NUTRITIONIST_SUB);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		return invitation;
	}

}
