package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;

@ExtendWith(MockitoExtension.class)
class PatientInvitationCreateServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-create-invite";

	private static final CreatePatientInvitationRequest REQUEST = new CreatePatientInvitationRequest("María López",
			"maria@example.com", LocalDate.of(1990, 5, 15), "F", "+525512345678", null, "María");

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private PatientInvitationTokenService patientInvitationTokenService;

	@Mock
	private PatientInvitationEmailSender patientInvitationEmailSender;

	private PatientInvitationProperties invitationProperties;

	private PatientInvitationCreateServiceImpl service;

	@BeforeEach
	void setUp() {
		invitationProperties = new PatientInvitationProperties();
		invitationProperties.setBaseUrl("https://links.test.example");
		service = new PatientInvitationCreateServiceImpl(subscriptionEntitlementService, pacienteRepository,
				patientInvitationRepository, patientInvitationTokenService, invitationProperties,
				patientInvitationEmailSender);
	}

	@Test
	void createInvitation_persistsHashOnlyAndReturnsLinkAndCode() {
		when(patientInvitationTokenService.generate())
			.thenReturn(new PatientInvitationTokenBundle("url-token-value", "NUTRI-ABCD-EFGH", "abc123hash"));
		when(pacienteRepository.existsByAssignedId(any())).thenReturn(false);
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			paciente.setId(100L);
			return paciente;
		});
		when(patientInvitationRepository.save(any(PatientInvitation.class))).thenAnswer(invocation -> {
			final PatientInvitation invitation = invocation.getArgument(0);
			invitation.setId(55L);
			return invitation;
		});

		final CreatedPatientInvitationResult created = service.createInvitation(NUTRITIONIST_SUB, REQUEST);

		assertThat(created.invitationId()).isEqualTo(55L);
		assertThat(created.pacienteId()).isEqualTo(100L);
		assertThat(created.inviteUrl()).isEqualTo("https://links.test.example/links/i/url-token-value");
		assertThat(created.humanCode()).isEqualTo("NUTRI-ABCD-EFGH");
		assertThat(created.expiresAt()).isAfter(Instant.now());

		final ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(pacienteCaptor.capture());
		assertThat(pacienteCaptor.getValue().getStatus()).isEqualTo(PacienteStatus.INVITED);
		assertThat(pacienteCaptor.getValue().getUserId()).isEqualTo(NUTRITIONIST_SUB);
		assertThat(pacienteCaptor.getValue().getEmail()).isEqualTo("maria@example.com");
		assertThat(pacienteCaptor.getValue().getPatientAuthSub()).isNull();

		final ArgumentCaptor<PatientInvitation> invitationCaptor = ArgumentCaptor.forClass(PatientInvitation.class);
		verify(patientInvitationRepository).save(invitationCaptor.capture());
		assertThat(invitationCaptor.getValue().getTokenHash()).isEqualTo("abc123hash");
		assertThat(invitationCaptor.getValue().getStatus()).isEqualTo(PatientInvitationStatus.PENDING);
		assertThat(invitationCaptor.getValue().getNutritionistUserId()).isEqualTo(NUTRITIONIST_SUB);

		verify(patientInvitationEmailSender).sendPatientInvitation(eq("maria@example.com"), eq("NUTRI-ABCD-EFGH"),
				eq("https://links.test.example/links/i/url-token-value"));
		verify(subscriptionEntitlementService).assertCanCreatePatient(NUTRITIONIST_SUB);
	}

	@Test
	void createInvitation_rejectsDuplicateAssignedId() {
		when(pacienteRepository.existsByAssignedId("CLINIC-1")).thenReturn(true);
		final CreatePatientInvitationRequest request = new CreatePatientInvitationRequest(REQUEST.name(),
				REQUEST.email(), REQUEST.dob(), REQUEST.gender(), REQUEST.phone(), "CLINIC-1", REQUEST.displayName());

		assertThatThrownBy(() -> service.createInvitation(NUTRITIONIST_SUB, request))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.CONFLICT.value());
	}

	@Test
	void createInvitation_propagatesSubscriptionLimit() {
		doThrow(new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_PATIENT_LIMIT, 10))
			.when(subscriptionEntitlementService)
			.assertCanCreatePatient(NUTRITIONIST_SUB);

		assertThatThrownBy(() -> service.createInvitation(NUTRITIONIST_SUB, REQUEST))
			.isInstanceOf(SubscriptionLimitExceededException.class);
	}

	@Test
	void createInvitation_setsExpiryFromProperties() {
		invitationProperties.setExpiryDays(7);
		when(patientInvitationTokenService.generate())
			.thenReturn(new PatientInvitationTokenBundle("token", "NUTRI-AAAA-BBBB", "hash"));
		when(pacienteRepository.existsByAssignedId(any())).thenReturn(false);
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			paciente.setId(1L);
			return paciente;
		});
		when(patientInvitationRepository.save(any(PatientInvitation.class))).thenAnswer(invocation -> {
			final PatientInvitation invitation = invocation.getArgument(0);
			invitation.setId(2L);
			return invitation;
		});

		final Instant before = Instant.now();
		final CreatedPatientInvitationResult created = service.createInvitation(NUTRITIONIST_SUB, REQUEST);
		final Instant after = Instant.now().plus(7, ChronoUnit.DAYS).plusSeconds(5);

		assertThat(created.expiresAt()).isBetween(before.plus(7, ChronoUnit.DAYS).minusSeconds(5), after);
	}

	@Test
	void createInvitation_includesOfflineJwsWhenConfigured() {
		invitationProperties.setJwsSecret("test-secret-at-least-32-characters-long");
		when(patientInvitationTokenService.generate())
			.thenReturn(new PatientInvitationTokenBundle("token", "NUTRI-CCCC-DDDD", "hash"));
		when(patientInvitationTokenService.createOfflineJws(eq(9L), eq("token"), any(Instant.class)))
			.thenReturn(Optional.of("offline.jws.token"));
		when(pacienteRepository.existsByAssignedId(any())).thenReturn(false);
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			paciente.setId(9L);
			return paciente;
		});
		when(patientInvitationRepository.save(any(PatientInvitation.class))).thenAnswer(invocation -> {
			final PatientInvitation invitation = invocation.getArgument(0);
			invitation.setId(3L);
			return invitation;
		});

		final CreatedPatientInvitationResult created = service.createInvitation(NUTRITIONIST_SUB, REQUEST);

		assertThat(created.offlineJws()).isEqualTo("offline.jws.token");
	}

}
