package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.profile.NutritionistProfileRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class MobilePhiLoggingIntegrationTest {

	private static final String PHI_MESSAGE_BODY = "PHI-LEAK-TEST-secret-message-body-xyz789";

	private static final String PATIENT_SUB = "auth0|mobile-phi-logging-integration-patient-sub";

	@InjectMocks
	private MobilePatientMessageService service;

	@Mock
	private PatientMessageRepository patientMessageRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Mock
	private PatientWriteRateLimiter patientWriteRateLimiter;

	private ListAppender<ILoggingEvent> logAppender;

	private Logger serviceLogger;

	@BeforeEach
	void attachLogAppender() {
		serviceLogger = (Logger) LoggerFactory.getLogger(MobilePatientMessageService.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		serviceLogger.addAppender(logAppender);
		serviceLogger.setLevel(Level.DEBUG);
	}

	@AfterEach
	void detachLogAppender() {
		if (serviceLogger != null && logAppender != null) {
			serviceLogger.detachAppender(logAppender);
		}
	}

	@Test
	void sendMessage_doesNotLogMessageBodyOrRawAuthSub() throws Exception {
		final PacienteAuthView authView = new PacienteAuthView() {
			@Override
			public Long getId() {
				return 12L;
			}

			@Override
			public String getPatientAuthSub() {
				return PATIENT_SUB;
			}

			@Override
			public String getUserId() {
				return "auth0|nutritionist-owner";
			}
		};
		final Paciente pacienteRef = new Paciente();
		pacienteRef.setId(12L);
		when(pacienteRepository.getReferenceById(12L)).thenReturn(pacienteRef);
		when(patientWriteRateLimiter.execute(eq(PatientWriteRateLimiter.PATIENT_MESSAGES), eq(PATIENT_SUB),
				any(Callable.class)))
			.thenAnswer(invocation -> {
				final Callable<?> callable = invocation.getArgument(2);
				return callable.call();
			});
		when(patientMessageRepository.save(any(PatientMessage.class))).thenAnswer(invocation -> {
			final PatientMessage saved = invocation.getArgument(0);
			saved.setId(501L);
			saved.setSentAt(Instant.parse("2026-06-15T12:00:00Z"));
			saved.setReadByPatient(true);
			saved.setReadByNutritionist(false);
			return saved;
		});

		service.sendMessage(authView, PHI_MESSAGE_BODY);

		assertThat(logAppender.list).isNotEmpty();
		for (final ILoggingEvent event : logAppender.list) {
			final String formatted = event.getFormattedMessage();
			assertThat(formatted).doesNotContain(PHI_MESSAGE_BODY);
			assertThat(formatted).doesNotContain(PATIENT_SUB);
			assertThat(formatted).contains("PatientMessage[id=501]");
		}
	}

}
