package com.nutriconsultas.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Structured, redacted INFO/WARN audit lines for the AI nutrition assistant (#397).
 * Aligns with {@code docs/ai/DATA-ACCESS-RULES.md#logging-and-audit-397}.
 */
@Component
@Slf4j
public final class AiAuditLogger {

	private final AiUsageMetrics usageMetrics;

	public AiAuditLogger(final AiUsageMetrics usageMetrics) {
		this.usageMetrics = usageMetrics;
	}

	public void logThreadCreated(final long threadId, @Nullable final String nutritionistId,
			final boolean patientLinked) {
		if (log.isInfoEnabled()) {
			log.info("AI audit event=thread_created threadId={} nutritionist={} patientLinked={}", threadId,
					LogRedaction.redactUserId(nutritionistId), patientLinked);
		}
	}

	public void logChatRequest(final long threadId, @Nullable final String nutritionistId, final AiChatRequestMode mode,
			final int messageLength, final boolean patientContext, final boolean dietaContext,
			final boolean platilloContext) {
		usageMetrics.recordChatMessage(mode);
		if (log.isInfoEnabled()) {
			log.info(
					"AI audit event=chat_request threadId={} nutritionist={} mode={} messageLength={} patientContext={} dietaContext={} platilloContext={}",
					threadId, LogRedaction.redactUserId(nutritionistId), mode, messageLength, patientContext,
					dietaContext, platilloContext);
		}
	}

	public void logOrchestrationComplete(final long threadId, @Nullable final String nutritionistId,
			final int toolCallCount, final List<String> toolNames, @Nullable final OpenAiTokenUsage tokenUsage) {
		usageMetrics.recordToolCalls(toolNames);
		usageMetrics.recordTokenUsage(tokenUsage);
		if (log.isInfoEnabled()) {
			final String tools = formatToolNames(toolNames);
			final Integer promptTokens = tokenUsage != null ? tokenUsage.promptTokens() : null;
			final Integer completionTokens = tokenUsage != null ? tokenUsage.completionTokens() : null;
			log.info(
					"AI audit event=orchestration_complete threadId={} nutritionist={} toolCalls={} tools={} promptTokens={} completionTokens={}",
					threadId, LogRedaction.redactUserId(nutritionistId), toolCallCount, tools, promptTokens,
					completionTokens);
		}
	}

	public void logOrchestrationShortCircuit(final long threadId, @Nullable final String nutritionistId,
			final String sourceLabel, final String sourceDetail) {
		if (log.isInfoEnabled()) {
			log.info("AI audit event=orchestration_short_circuit threadId={} nutritionist={} source={} detail={}",
					threadId, LogRedaction.redactUserId(nutritionistId), sourceLabel,
					AiAuditRedaction.redactSecrets(sourceDetail));
		}
	}

	public void logToolRejected(final long threadId, final String toolName) {
		if (log.isWarnEnabled()) {
			log.warn("AI audit event=tool_rejected threadId={} tool={}", threadId, toolName);
		}
	}

	public void logToolDispatchFailed(final long threadId, final String toolName) {
		if (log.isWarnEnabled()) {
			log.warn("AI audit event=tool_dispatch_failed threadId={} tool={}", threadId, toolName);
		}
	}

	public void logMcpToolCall(final long threadId, final String nutritionistId, final String mcpToolName,
			final String internalToolName, final boolean success) {
		if (log.isInfoEnabled()) {
			log.info("AI audit event=mcp_tool_call threadId={} nutritionist={} mcpTool={} internalTool={} success={}",
					threadId, LogRedaction.redactUserId(nutritionistId), mcpToolName, internalToolName, success);
		}
	}

	public void logMaxToolCallsReached(final long threadId, final int maxToolCalls) {
		if (log.isWarnEnabled()) {
			log.warn("AI audit event=max_tool_calls threadId={} maxToolCalls={}", threadId, maxToolCalls);
		}
	}

	public void logDraftCreated(final long draftId, final long threadId, final AiDraftType draftType) {
		usageMetrics.recordDraftCreated(draftType);
		if (log.isInfoEnabled()) {
			log.info("AI audit event=draft_created draftId={} threadId={} type={}", draftId, threadId, draftType);
		}
	}

	public void logDraftAccepted(final long draftId, final long threadId, final AiDraftStatus status) {
		usageMetrics.recordDraftAccepted();
		if (log.isInfoEnabled()) {
			log.info("AI audit event=draft_accepted draftId={} threadId={} status={}", draftId, threadId, status);
		}
	}

	public void logDraftMaterialized(final long draftId, final long threadId, final AiDraftCreatedEntityType entityType,
			final long entityId) {
		if (log.isInfoEnabled()) {
			log.info("AI audit event=draft_materialized draftId={} threadId={} entityType={} entityId={}", draftId,
					threadId, entityType, entityId);
		}
	}

	public void logDraftDiscarded(final long draftId, final long threadId) {
		usageMetrics.recordDraftDiscarded();
		if (log.isInfoEnabled()) {
			log.info("AI audit event=draft_discarded draftId={} threadId={}", draftId, threadId);
		}
	}

	public void logAccessDenied(@Nullable final String nutritionistId, final String reason) {
		if (log.isInfoEnabled()) {
			log.info("AI audit event=access_denied nutritionist={} reason={}",
					LogRedaction.redactUserId(nutritionistId), reason);
		}
	}

	public void logOpenAiError(@Nullable final Long threadId, final String errorKind, final int httpStatus) {
		usageMetrics.recordOpenAiError(errorKind);
		if (OpenAiClientException.ErrorKind.RATE_LIMIT.name().equals(errorKind)) {
			usageMetrics.recordOpenAiRateLimited();
		}
		if (log.isWarnEnabled()) {
			log.warn("AI audit event=openai_error threadId={} errorKind={} httpStatus={}", threadId, errorKind,
					httpStatus);
		}
	}

	public void logInputBlocked(final String category, final int messageLength) {
		if (log.isWarnEnabled()) {
			log.warn("AI audit event=input_blocked category={} messageLength={}", category, messageLength);
		}
	}

	public void logThreadTruncated(final long threadId, final long fromMessageId, final int removedMessages,
			final int discardedDrafts) {
		if (log.isInfoEnabled()) {
			log.info(
					"AI audit event=thread_truncated threadId={} fromMessageId={} removedMessages={} discardedDrafts={}",
					threadId, fromMessageId, removedMessages, discardedDrafts);
		}
	}

	private static String formatToolNames(final List<String> toolNames) {
		if (toolNames == null || toolNames.isEmpty()) {
			return "[]";
		}
		final List<String> sanitized = toolNames.stream()
			.filter(StringUtils::hasText)
			.map(AiAuditRedaction::redactSecrets)
			.collect(Collectors.toList());
		return sanitized.toString();
	}

}
