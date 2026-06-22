package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.nutriconsultas.subscription.PlanTier;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class ConsoleInvitationEmailSenderTest {

	private static final String INVITE_URL = "http://localhost:3000/invitation/nutritionist/redeem?token=abc";

	@Mock
	private InvitationEmailTemplateRenderer templateRenderer;

	private ConsoleInvitationEmailSender sender;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		sender = new ConsoleInvitationEmailSender(templateRenderer);
		when(templateRenderer.renderHtmlBody(PlanTier.BASICO, INVITE_URL)).thenReturn("<html>invite</html>");

		final Logger logger = (Logger) LoggerFactory.getLogger(ConsoleInvitationEmailSender.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@Test
	void sendNutritionistInvitationLogsInvitationLinkMarker() {
		sender.sendNutritionistInvitation("invitee@example.com", PlanTier.BASICO, INVITE_URL);

		verify(templateRenderer).renderHtmlBody(PlanTier.BASICO, INVITE_URL);
		assertThat(logAppender.list)
			.anyMatch(event -> event.getFormattedMessage().equals("INVITATION_LINK=" + INVITE_URL));
		assertThat(logAppender.list).noneMatch(event -> event.getFormattedMessage().contains("invitee@example.com"));
	}

}
