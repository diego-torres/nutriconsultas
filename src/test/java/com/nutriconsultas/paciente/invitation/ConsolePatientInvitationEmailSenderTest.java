package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class ConsolePatientInvitationEmailSenderTest {

	private static final String HUMAN_CODE = "NUTRI-ABCD-EFGH";

	private static final String INVITE_URL = "http://10.0.2.2:3000/links/i/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

	@Mock
	private PatientInvitationEmailTemplateRenderer templateRenderer;

	private ConsolePatientInvitationEmailSender sender;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		sender = new ConsolePatientInvitationEmailSender(templateRenderer);

		final Logger logger = (Logger) LoggerFactory.getLogger(ConsolePatientInvitationEmailSender.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@Test
	void sendPatientInvitationLogsPatientInvitationLinkMarker() {
		when(templateRenderer.renderHtmlBody(HUMAN_CODE, INVITE_URL)).thenReturn("<html>invite</html>");
		sender.sendPatientInvitation("patient@example.com", HUMAN_CODE, INVITE_URL);

		verify(templateRenderer).renderHtmlBody(HUMAN_CODE, INVITE_URL);
		assertThat(logAppender.list)
			.anyMatch(event -> event.getFormattedMessage().equals("PATIENT_INVITATION_LINK=" + INVITE_URL));
		assertThat(logAppender.list).noneMatch(event -> event.getFormattedMessage().contains("patient@example.com"));
		assertThat(logAppender.list).noneMatch(event -> event.getFormattedMessage().contains(HUMAN_CODE));
	}

}
