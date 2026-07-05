package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer counters for AI assistant usage (#398). Tags are low-cardinality only —
 * never nutritionist IDs, thread IDs, patient data, or message content.
 */
@Component
public final class AiUsageMetrics {

	static final String CHAT_MESSAGES = "ai.chat.messages";

	static final String DRAFTS_CREATED = "ai.drafts.created";

	static final String DRAFTS_ACCEPTED = "ai.drafts.accepted";

	static final String DRAFTS_DISCARDED = "ai.drafts.discarded";

	static final String OPENAI_ERRORS = "ai.openai.errors";

	static final String RATE_LIMITED = "ai.rate_limited";

	static final String OPENAI_TOKENS = "ai.openai.tokens";

	static final String TOOL_CALLS = "ai.tool.calls";

	private static final String TAG_MODE = "mode";

	private static final String TAG_TYPE = "type";

	private static final String TAG_KIND = "kind";

	private static final String TAG_SOURCE = "source";

	private static final String TAG_TOOL = "tool";

	private static final String SOURCE_CHAT = "chat";

	private static final String SOURCE_OPENAI = "openai";

	private final MeterRegistry meterRegistry;

	public AiUsageMetrics(final MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordChatMessage(final AiChatRequestMode mode) {
		counter(CHAT_MESSAGES, TAG_MODE, safeEnum(mode)).increment();
	}

	public void recordDraftCreated(final AiDraftType draftType) {
		counter(DRAFTS_CREATED, TAG_TYPE, safeEnum(draftType)).increment();
	}

	public void recordDraftAccepted() {
		counter(DRAFTS_ACCEPTED).increment();
	}

	public void recordDraftDiscarded() {
		counter(DRAFTS_DISCARDED).increment();
	}

	public void recordOpenAiError(final String errorKind) {
		counter(OPENAI_ERRORS, TAG_KIND, safeTagValue(errorKind)).increment();
	}

	public void recordChatRateLimited() {
		counter(RATE_LIMITED, TAG_SOURCE, SOURCE_CHAT).increment();
	}

	public void recordOpenAiRateLimited() {
		counter(RATE_LIMITED, TAG_SOURCE, SOURCE_OPENAI).increment();
	}

	public void recordTokenUsage(@Nullable final OpenAiTokenUsage tokenUsage) {
		if (tokenUsage == null) {
			return;
		}
		if (tokenUsage.promptTokens() > 0) {
			counter(OPENAI_TOKENS, TAG_KIND, "prompt").increment(tokenUsage.promptTokens());
		}
		if (tokenUsage.completionTokens() > 0) {
			counter(OPENAI_TOKENS, TAG_KIND, "completion").increment(tokenUsage.completionTokens());
		}
	}

	public void recordToolCalls(final java.util.List<String> toolNames) {
		if (toolNames == null || toolNames.isEmpty()) {
			return;
		}
		for (final String toolName : toolNames) {
			if (StringUtils.hasText(toolName)) {
				counter(TOOL_CALLS, TAG_TOOL, toolName.trim()).increment();
			}
		}
	}

	private Counter counter(final String name, final String... tags) {
		return meterRegistry.counter(name, tags);
	}

	private static String safeEnum(@Nullable final Enum<?> value) {
		if (value == null) {
			return "unknown";
		}
		return value.name();
	}

	private static String safeTagValue(@Nullable final String value) {
		if (!StringUtils.hasText(value)) {
			return "unknown";
		}
		return value.trim();
	}

}
