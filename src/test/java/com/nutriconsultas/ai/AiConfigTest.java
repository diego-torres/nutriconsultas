package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

class AiConfigTest {

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		final Logger logger = (Logger) LoggerFactory.getLogger(AiConfig.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
		logger.setLevel(Level.INFO);
	}

	@AfterEach
	void tearDown() {
		final Logger logger = (Logger) LoggerFactory.getLogger(AiConfig.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void startupLogNeverContainsApiKey() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setApiKey("sk-super-secret-key");
		properties.getOpenai().setModel("gpt-5.5");

		final AiConfig config = new AiConfig(properties);
		config.logAiConfiguration();

		assertThat(logAppender.list).isNotEmpty();
		assertThat(logAppender.list)
			.allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("sk-super-secret-key"));
	}

	@Test
	void misconfiguredStartupLogsWarningWithoutSecret() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setApiKey("sk-super-secret-key");

		final AiConfig config = new AiConfig(properties);
		config.logAiConfiguration();

		assertThat(logAppender.list).anyMatch(event -> event.getLevel() == Level.WARN
				&& event.getFormattedMessage().contains("OPENAI_API_KEY or OPENAI_MODEL missing"));
		assertThat(logAppender.list)
			.allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("sk-super-secret-key"));
	}

}
