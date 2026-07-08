package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
		"nutriconsultas.apple.signin.auto-process-destructive-events=true" })
class AppleSignInDestructiveAutoProcessIntegrationTest {

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
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);
	}

	@Test
	void processesAccountDeleteWithDestructiveAutoProcessEnabled() {
		final Paciente paciente = AppleSignInIntegrationTestSupport.persistApplePaciente(pacienteRepository);
		final String signedPayload = AppleSignInIntegrationTestSupport.signSampleDestructiveEvent("evt-delete-auto",
				"account-delete");

		notificationService.handleNotification(signedPayload);

		assertThat(notificationRepository.findByAppleEventId("evt-delete-auto")).isPresent()
			.get()
			.satisfies(notification -> {
				assertThat(notification.getLifecycleAction())
					.isEqualTo(AppleSignInLifecycleAction.APPLIED_PENDING_DELETION_REVIEW);
				assertThat(notification.getPacienteId()).isEqualTo(paciente.getId());
			});
		final Paciente reloaded = pacienteRepository.findById(paciente.getId()).orElseThrow();
		assertThat(reloaded.getAppleLifecycleStatus()).isEqualTo(ApplePacienteLifecycleStatus.PENDING_DELETION_REVIEW);
		verify(auth0ManagementUserService).updateAppMetadata(eq(AppleSignInIntegrationTestSupport.AUTH0_USER_ID),
				any(Map.class));
		verify(auth0ManagementUserService, never()).deleteUser(any());
	}

}
