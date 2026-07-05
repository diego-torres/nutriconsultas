package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

class AiAuditLoggerTest {

	private ListAppender<ILoggingEvent> logAppender;

	private AiAuditLogger auditLogger;

	@BeforeEach
	void setUp() {
		final Logger logger = (Logger) LoggerFactory.getLogger(AiAuditLogger.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
		logger.setLevel(Level.INFO);
		auditLogger = new AiAuditLogger();
	}

	@AfterEach
	void tearDown() {
		final Logger logger = (Logger) LoggerFactory.getLogger(AiAuditLogger.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void chatRequestLogsMetadataWithoutMessageBody() {
		final String secretMessage = "Paciente Juan Pérez alergia mariscos sk-proj-leaked-key-abcdefghij";

		auditLogger.logChatRequest(12L, "auth0|nutritionist-secret", AiChatRequestMode.SEND,
				AiAuditRedaction.safeMessageLength(secretMessage), true, false, false);

		assertThat(logAppender.list).hasSize(1);
		final String formatted = logAppender.list.get(0).getFormattedMessage();
		assertThat(formatted).contains("event=chat_request");
		assertThat(formatted).contains("threadId=12");
		assertThat(formatted).contains("messageLength=" + secretMessage.length());
		assertThat(formatted).doesNotContain(secretMessage);
		assertThat(formatted).doesNotContain("sk-proj-leaked-key-abcdefghij");
		assertThat(formatted).doesNotContain("Juan Pérez");
	}

	@Test
	void orchestrationCompleteLogsToolNamesWithoutSecrets() {
		auditLogger.logOrchestrationComplete(7L, "auth0|abc123", 2, List.of("search_food_catalog", "create_dish_draft"),
				new OpenAiTokenUsage(100, 50, 150));

		assertThat(logAppender.list).hasSize(1);
		final String formatted = logAppender.list.get(0).getFormattedMessage();
		assertThat(formatted).contains("event=orchestration_complete");
		assertThat(formatted).contains("tools=[search_food_catalog, create_dish_draft]");
		assertThat(formatted).contains("promptTokens=100");
		assertThat(formatted).contains("completionTokens=50");
	}

	@Test
	void draftLifecycleEventsUseStructuredPrefix() {
		auditLogger.logDraftCreated(3L, 9L, AiDraftType.DISH);
		auditLogger.logDraftAccepted(3L, 9L, AiDraftStatus.ACCEPTED);
		auditLogger.logDraftDiscarded(4L, 9L);

		assertThat(logAppender.list).hasSize(3);
		assertThat(logAppender.list.get(0).getFormattedMessage()).contains("event=draft_created");
		assertThat(logAppender.list.get(1).getFormattedMessage()).contains("event=draft_accepted");
		assertThat(logAppender.list.get(2).getFormattedMessage()).contains("event=draft_discarded");
	}

	@Test
	void accessDeniedRedactsNutritionistId() {
		auditLogger.logAccessDenied("auth0|full-nutritionist-id", "missing_entitlement");

		assertThat(logAppender.list).hasSize(1);
		final String formatted = logAppender.list.get(0).getFormattedMessage();
		assertThat(formatted).contains("event=access_denied");
		assertThat(formatted).contains("reason=missing_entitlement");
		assertThat(formatted).doesNotContain("full-nutritionist-id");
	}

}
