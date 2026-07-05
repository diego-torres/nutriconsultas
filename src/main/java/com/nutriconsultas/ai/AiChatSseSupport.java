package com.nutriconsultas.ai;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Helpers for AI chat Server-Sent Events (#435).
 */
public final class AiChatSseSupport {

	private AiChatSseSupport() {
	}

	public static void sendStatus(final SseEmitter emitter, final String phase, final String message)
			throws IOException {
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("phase", phase);
		if (message != null) {
			payload.put("message", message);
		}
		emitter.send(SseEmitter.event().name("status").data(AiToolJsonSerializer.toJson(payload)));
	}

	public static void sendDelta(final SseEmitter emitter, final String content) throws IOException {
		final Map<String, Object> payload = Map.of("content", content);
		emitter.send(SseEmitter.event().name("delta").data(AiToolJsonSerializer.toJson(payload)));
	}

	public static void sendDone(final SseEmitter emitter, final AiOrchestrationResult result) throws IOException {
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("success", true);
		payload.put("threadId", result.threadId());
		payload.put("assistantMessageId", result.assistantMessage().getId());
		payload.put("content", result.assistantMessage().getContent());
		payload.put("toolCallsExecuted", result.toolCallsExecuted());
		if (result.tokenUsage() != null) {
			payload.put("tokenUsage", tokenUsageMap(result.tokenUsage()));
		}
		emitter.send(SseEmitter.event().name("done").data(AiToolJsonSerializer.toJson(payload)));
	}

	public static void sendError(final SseEmitter emitter, final String message) throws IOException {
		sendError(emitter, AiToolErrorCode.INTERNAL, message);
	}

	public static void sendError(final SseEmitter emitter, final AiToolErrorCode errorCode, final String message)
			throws IOException {
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("success", false);
		payload.put("errorCode", errorCode.name());
		payload.put("message", message);
		emitter.send(SseEmitter.event().name("error").data(AiToolJsonSerializer.toJson(payload)));
	}

	public static void completeQuietly(final SseEmitter emitter) {
		try {
			emitter.complete();
		}
		catch (Exception ex) {
			emitter.completeWithError(ex);
		}
	}

	private static Map<String, Object> tokenUsageMap(final OpenAiTokenUsage usage) {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("promptTokens", usage.promptTokens());
		map.put("completionTokens", usage.completionTokens());
		map.put("totalTokens", usage.totalTokens());
		return map;
	}

}
