package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;

@ExtendWith(MockitoExtension.class)
class PatientMobileInvitationServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-web-invite";

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private PatientInvitationTokenService patientInvitationTokenService;

	@Mock
	private PatientInvitationEmailSender patientInvitationEmailSender;

	@Mock
	private PatientInvitationRevokeService patientInvitationRevokeService;

	private PatientInvitationProperties invitationProperties;

	private PatientMobileInvitationServiceImpl service;

	@BeforeEach
	void setUp() {
		invitationProperties = new PatientInvitationProperties();
		invitationProperties.setBaseUrl("https://links.test.example");
		service = new PatientMobileInvitationServiceImpl(pacienteRepository, patientInvitationRepository,
				patientInvitationTokenService, invitationProperties, patientInvitationEmailSender,
				patientInvitationRevokeService);
	}

	@Test
	void getStatus_returnsNoneForActivePatientWithoutAuth() {
		final Paciente paciente = activePaciente(1L, "patient@test.com");
		when(pacienteRepository.findByIdAndUserId(1L, NUTRITIONIST_SUB)).thenReturn(Optional.of(paciente));
		when(patientInvitationRepository.findByPacienteIdAndStatus(1L, PatientInvitationStatus.PENDING))
			.thenReturn(List.of());

		final PatientMobileInvitationStatus status = service.getStatus(1L, NUTRITIONIST_SUB);

		assertThat(status.stateCode()).isEqualTo("NONE");
		assertThat(status.canSend()).isTrue();
	}

	@Test
	void sendInvitation_transitionsActiveToInvitedAndEmailsPatient() {
		final Paciente paciente = activePaciente(2L, "invite@test.com");
		when(pacienteRepository.findByIdAndUserId(2L, NUTRITIONIST_SUB)).thenReturn(Optional.of(paciente));
		when(patientInvitationRepository.findByPacienteIdAndStatus(2L, PatientInvitationStatus.PENDING))
			.thenReturn(List.of());
		when(patientInvitationTokenService.generate())
			.thenReturn(new PatientInvitationTokenBundle("url-token", "NUTRI-WEB-TEST", "hash123"));
		when(pacienteRepository.existsByAssignedId(any())).thenReturn(false);
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientInvitationRepository.save(any(PatientInvitation.class))).thenAnswer(invocation -> {
			final PatientInvitation invitation = invocation.getArgument(0);
			invitation.setId(77L);
			return invitation;
		});

		final IssuedPatientMobileInvitationResult issued = service.sendInvitation(2L, NUTRITIONIST_SUB);

		assertThat(issued.invitationId()).isEqualTo(77L);
		assertThat(issued.humanCode()).isEqualTo("NUTRI-WEB-TEST");

		final ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(pacienteCaptor.capture());
		assertThat(pacienteCaptor.getValue().getStatus()).isEqualTo(PacienteStatus.INVITED);

		verify(patientInvitationEmailSender).sendPatientInvitation(eq("invite@test.com"), eq("NUTRI-WEB-TEST"),
				eq("https://links.test.example/links/i/url-token"));
	}

	@Test
	void sendInvitation_rejectsPatientWithoutEmail() {
		final Paciente paciente = activePaciente(3L, null);
		when(pacienteRepository.findByIdAndUserId(3L, NUTRITIONIST_SUB)).thenReturn(Optional.of(paciente));
		when(patientInvitationRepository.findByPacienteIdAndStatus(3L, PatientInvitationStatus.PENDING))
			.thenReturn(List.of());

		assertThatThrownBy(() -> service.sendInvitation(3L, NUTRITIONIST_SUB))
			.isInstanceOf(PatientMobileInvitationNotAllowedException.class)
			.extracting(ex -> ((PatientMobileInvitationNotAllowedException) ex).getMessageKey())
			.isEqualTo("NO_EMAIL");
	}

	@Test
	void revokePendingInvitation_delegatesToRevokeService() {
		final Paciente paciente = activePaciente(4L, "pending@test.com");
		paciente.setStatus(PacienteStatus.INVITED);
		final PatientInvitation pending = new PatientInvitation();
		pending.setId(88L);
		pending.setStatus(PatientInvitationStatus.PENDING);
		pending.setExpiresAt(Instant.now().plus(3, ChronoUnit.DAYS));
		when(pacienteRepository.findByIdAndUserId(4L, NUTRITIONIST_SUB)).thenReturn(Optional.of(paciente));
		when(patientInvitationRepository.findByPacienteIdAndStatus(4L, PatientInvitationStatus.PENDING))
			.thenReturn(List.of(pending));
		when(patientInvitationRevokeService.revoke(88L, NUTRITIONIST_SUB))
			.thenReturn(new PatientInvitationRevokeResult(88L, 4L, PatientInvitationStatus.REVOKED));

		final PatientInvitationRevokeResult result = service.revokePendingInvitation(4L, NUTRITIONIST_SUB);

		assertThat(result.invitationId()).isEqualTo(88L);
		verify(patientInvitationRevokeService).revoke(88L, NUTRITIONIST_SUB);
	}

	private static Paciente activePaciente(final Long id, final String email) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setUserId(NUTRITIONIST_SUB);
		paciente.setName("Paciente Test");
		paciente.setEmail(email);
		paciente.setStatus(PacienteStatus.ACTIVE);
		return paciente;
	}

}
