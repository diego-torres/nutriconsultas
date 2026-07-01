package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPropertiesTest {

	@Test
	void disabledByDefaultIsNotOperational() {
		final AiProperties properties = new AiProperties();

		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.isOperational()).isFalse();
		assertThat(properties.isEnabledButMisconfigured()).isFalse();
	}

	@Test
	void enabledWithoutApiKeyIsMisconfigured() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setModel("gpt-5.5");

		assertThat(properties.isOpenAiConfigured()).isFalse();
		assertThat(properties.isOperational()).isFalse();
		assertThat(properties.isEnabledButMisconfigured()).isTrue();
	}

	@Test
	void enabledWithoutModelIsMisconfigured() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setApiKey("sk-test-key");

		assertThat(properties.isOpenAiConfigured()).isFalse();
		assertThat(properties.isEnabledButMisconfigured()).isTrue();
	}

	@Test
	void operationalWhenEnabledWithKeyAndModel() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setApiKey("sk-test-key");
		properties.getOpenai().setModel("gpt-5.5");

		assertThat(properties.isOperational()).isTrue();
		assertThat(properties.isEnabledButMisconfigured()).isFalse();
	}

	@Test
	void trimsApiKeyAndModel() {
		final AiProperties properties = new AiProperties();
		properties.getOpenai().setApiKey("  sk-test  ");
		properties.getOpenai().setModel("  gpt-5.5  ");

		assertThat(properties.getOpenai().getApiKey()).isEqualTo("sk-test");
		assertThat(properties.getOpenai().getModel()).isEqualTo("gpt-5.5");
	}

	@Test
	void maxToolCallsClampedToSafeRange() {
		final AiProperties properties = new AiProperties();

		properties.setMaxToolCalls(0);
		assertThat(properties.getMaxToolCalls()).isEqualTo(1);

		properties.setMaxToolCalls(100);
		assertThat(properties.getMaxToolCalls()).isEqualTo(32);

		properties.setMaxToolCalls(8);
		assertThat(properties.getMaxToolCalls()).isEqualTo(8);
	}

	@Test
	void misconfigurationUserMessageIsSpanish() {
		final AiProperties properties = new AiProperties();

		assertThat(properties.getMisconfigurationUserMessage()).contains("asistente de IA");
	}

}
