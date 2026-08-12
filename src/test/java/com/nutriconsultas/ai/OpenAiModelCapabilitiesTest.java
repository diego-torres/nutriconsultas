package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenAiModelCapabilitiesTest {

	@Test
	void detectsGpt5FamilyAsReasoningStyle() {
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("gpt-5-mini")).isTrue();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("GPT-5")).isTrue();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("gpt-5.2-chat")).isTrue();
	}

	@Test
	void detectsOSeriesAsReasoningStyle() {
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("o1")).isTrue();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("o3-mini")).isTrue();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("o4-mini")).isTrue();
	}

	@Test
	void leavesClassicChatModelsAsNonReasoning() {
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("gpt-4o")).isFalse();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("gpt-4.1-mini")).isFalse();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("gpt-test")).isFalse();
	}

	@Test
	void blankModelIsNotReasoningStyle() {
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel(null)).isFalse();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("")).isFalse();
		assertThat(OpenAiModelCapabilities.isReasoningStyleModel("   ")).isFalse();
	}

}
