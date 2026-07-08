package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(AppleSignInIntegrationTestSupport.TestJwksConfiguration.class)
@TestPropertySource(properties = { "nutriconsultas.apple.signin.webhook.enabled=true",
		"nutriconsultas.apple.signin.expected-audience=com.minutriporcion.app",
		"nutriconsultas.apple.signin.auto-process-destructive-events=false" })
class AppleSignInNotificationFlowIntegrationTest {

	@Autowired
	private AppleSignInNotificationService notificationService;

	@Autowired
	private AppleSignInNotificationRepository notificationRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	@MockBean
	private Auth0ManagementUserService auth0ManagementUserService;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
		pacienteRepository.deleteAll();
		when(auth0ManagementUserService.isConfigured()).thenReturn(false);
	}

	@Test
	void processesUnknownEventTypeAsIgnored() {
		final String signedPayload = AppleSignInIntegrationTestSupport.signNotification("evt-unknown-1",
				"future-event-type",
				Map.of("type", "future-event-type", "sub", AppleSignInIntegrationTestSupport.APPLE_SUBJECT));

		final AppleSignInWebhookOutcome outcome = notificationService.handleNotification(signedPayload);

		assertThat(outcome).isEqualTo(AppleSignInWebhookOutcome.PROCESSED);
		assertThat(notificationRepository.findByAppleEventId("evt-unknown-1")).isPresent()
			.get()
			.satisfies(notification -> {
				assertThat(notification.getEventType()).isEqualTo(AppleSignInEventType.UNKNOWN);
				assertThat(notification.getProcessingStatus())
					.isEqualTo(AppleSignInNotificationProcessingStatus.IGNORED);
			});
	}

	@Test
	void processesRelayEmailChangeEventForMappedPaciente() {
		AppleSignInIntegrationTestSupport.persistApplePaciente(pacienteRepository);
		final String signedPayload = AppleSignInIntegrationTestSupport.signNotification("evt-relay-1", "email-enabled",
				Map.of("type", "email-enabled", "sub", AppleSignInIntegrationTestSupport.APPLE_SUBJECT, "email",
						"relay@privaterelay.appleid.com", "is_private_email", true));

		final AppleSignInWebhookOutcome outcome = notificationService.handleNotification(signedPayload);

		assertThat(outcome).isEqualTo(AppleSignInWebhookOutcome.PROCESSED);
		assertThat(notificationRepository.findByAppleEventId("evt-relay-1")).isPresent()
			.get()
			.satisfies(notification -> {
				assertThat(notification.getLifecycleAction())
					.isEqualTo(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED);
				assertThat(notification.getPacienteId()).isNotNull();
			});
		final Paciente paciente = pacienteRepository.findByAppleSubject(AppleSignInIntegrationTestSupport.APPLE_SUBJECT)
			.orElseThrow();
		assertThat(paciente.getAppleRelayForwardingEnabled()).isTrue();
		verify(auth0ManagementUserService, never()).findUserByAppleSubject(any());
	}

	@Test
	void processesAccountDeleteInObserveOnlyModeWithoutLifecycleMutation() {
		final Paciente paciente = AppleSignInIntegrationTestSupport.persistApplePaciente(pacienteRepository);
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-delete-observe",
				"account-delete");

		notificationService.handleNotification(signedPayload);

		assertThat(notificationRepository.findByAppleEventId("evt-delete-observe")).isPresent()
			.get()
			.satisfies(notification -> {
				assertThat(notification.getLifecycleAction())
					.isEqualTo(AppleSignInLifecycleAction.SKIPPED_OBSERVE_ONLY);
				assertThat(notification.getPacienteId()).isEqualTo(paciente.getId());
			});
		final Paciente reloaded = pacienteRepository.findById(paciente.getId()).orElseThrow();
		assertThat(reloaded.getAppleLifecycleStatus()).isEqualTo(ApplePacienteLifecycleStatus.NONE);
	}

}
