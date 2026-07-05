package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AiUsageMetricsTest {

	private SimpleMeterRegistry registry;

	private AiUsageMetrics metrics;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		metrics = new AiUsageMetrics(registry);
	}

	@Test
	void recordsChatMessagesByModeWithoutPhiTags() {
		metrics.recordChatMessage(AiChatRequestMode.SEND);
		metrics.recordChatMessage(AiChatRequestMode.STREAM);

		assertThat(registry.get(AiUsageMetrics.CHAT_MESSAGES).tag("mode", "SEND").counter().count()).isEqualTo(1.0);
		assertThat(registry.get(AiUsageMetrics.CHAT_MESSAGES).tag("mode", "STREAM").counter().count()).isEqualTo(1.0);
		assertThat(registry.getMeters()).allSatisfy(meter -> {
			assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getKey().contains("nutritionist"));
			assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getKey().contains("thread"));
			assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getValue().contains("auth0|"));
		});
	}

	@Test
	void recordsDraftLifecycleCounters() {
		metrics.recordDraftCreated(AiDraftType.DISH);
		metrics.recordDraftAccepted();
		metrics.recordDraftDiscarded();

		assertThat(registry.get(AiUsageMetrics.DRAFTS_CREATED).tag("type", "DISH").counter().count()).isEqualTo(1.0);
		assertThat(registry.get(AiUsageMetrics.DRAFTS_ACCEPTED).counter().count()).isEqualTo(1.0);
		assertThat(registry.get(AiUsageMetrics.DRAFTS_DISCARDED).counter().count()).isEqualTo(1.0);
	}

	@Test
	void recordsOpenAiErrorsAndRateLimits() {
		metrics.recordOpenAiError(OpenAiClientException.ErrorKind.RATE_LIMIT.name());
		metrics.recordOpenAiRateLimited();
		metrics.recordChatRateLimited();

		assertThat(registry.get(AiUsageMetrics.OPENAI_ERRORS).tag("kind", "RATE_LIMIT").counter().count())
			.isEqualTo(1.0);
		assertThat(registry.get(AiUsageMetrics.RATE_LIMITED).tag("source", "openai").counter().count()).isEqualTo(1.0);
		assertThat(registry.get(AiUsageMetrics.RATE_LIMITED).tag("source", "chat").counter().count()).isEqualTo(1.0);
	}

	@Test
	void recordsTokenUsageAndToolCalls() {
		metrics.recordTokenUsage(new OpenAiTokenUsage(100, 40, 140));
		metrics.recordToolCalls(List.of("search_food_catalog", "create_dish_draft"));

		assertThat(registry.get(AiUsageMetrics.OPENAI_TOKENS).tag("kind", "prompt").counter().count()).isEqualTo(100.0);
		assertThat(registry.get(AiUsageMetrics.OPENAI_TOKENS).tag("kind", "completion").counter().count())
			.isEqualTo(40.0);
		assertThat(registry.get(AiUsageMetrics.TOOL_CALLS).tag("tool", "search_food_catalog").counter().count())
			.isEqualTo(1.0);
	}

	@Test
	void ignoresNullTokenUsage() {
		metrics.recordTokenUsage(null);

		assertThat(registry.find(AiUsageMetrics.OPENAI_TOKENS).counters()).isEmpty();
	}

	@Test
	void counterNamesUseAiPrefix() {
		metrics.recordDraftAccepted();
		final Counter counter = registry.find(AiUsageMetrics.DRAFTS_ACCEPTED).counter();
		assertThat(counter).isNotNull();
		assertThat(counter.getId().getName()).isEqualTo(AiUsageMetrics.DRAFTS_ACCEPTED);
	}

}
