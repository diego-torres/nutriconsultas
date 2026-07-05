package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/nutritionist/ai/chat")
@Slf4j
public class AiChatRestController {

	private final AiChatService chatService;

	private final AiChatRateLimiter aiChatRateLimiter;

	private final AiUsageMetrics aiUsageMetrics;

	public AiChatRestController(final AiChatService chatService, final AiChatRateLimiter aiChatRateLimiter,
			final AiUsageMetrics aiUsageMetrics) {
		this.chatService = chatService;
		this.aiChatRateLimiter = aiChatRateLimiter;
		this.aiUsageMetrics = aiUsageMetrics;
	}

	@PostMapping("/start")
	public ResponseEntity<Map<String, Object>> startChat(
			@RequestBody(required = false) final AiStartChatRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		final AiStartChatRequest body = request != null ? request
				: new AiStartChatRequest(null, null, null, null, null);
		try {
			final AiChatPromptContext promptContext = new AiChatPromptContext(body.patientId(), body.dietaId(),
					body.platilloId());
			final AiChatThread thread = chatService.startThread(nutritionistId, body.title(), body.patientId(),
					body.clinicId(), promptContext);
			final Map<String, Object> response = successBody();
			response.put("threadId", thread.getId());
			response.put("title", thread.getTitle());
			response.put("patientId", thread.getPatient() != null ? thread.getPatient().getId() : null);
			response.put("clinicId", thread.getClinicId());
			response.put("createdAt", thread.getCreatedAt());
			response.put("updatedAt", thread.getUpdatedAt());
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		}
		catch (final AiChatException ex) {
			return errorResponse(ex);
		}
	}

	@PostMapping("/message")
	public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody final AiSendMessageRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		if (request == null) {
			return errorResponse(HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION, "Solicitud no válida.");
		}
		try {
			final AiChatPromptContext promptContext = new AiChatPromptContext(request.patientId(), request.dietaId(),
					request.platilloId());
			final AiOrchestrationResult result = aiChatRateLimiter.executeMessage(nutritionistId, () -> chatService
				.sendMessage(nutritionistId, request.threadId(), request.message(), promptContext));
			final Map<String, Object> response = successBody();
			putOrchestrationFields(response, result);
			return ResponseEntity.ok(response);
		}
		catch (final AiChatException ex) {
			return errorResponse(ex);
		}
		catch (final AiOrchestrationException ex) {
			return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, AiToolErrorCode.VALIDATION, ex.getMessage());
		}
		catch (final OpenAiClientException ex) {
			return mapOpenAiException(ex);
		}
		catch (final RequestNotPermitted ex) {
			recordChatRateLimited("AI chat message rate limit exceeded");
			return errorResponse(HttpStatus.TOO_MANY_REQUESTS, AiToolErrorCode.RATE_LIMIT,
					AiChatRateLimiter.RATE_LIMIT_USER_MESSAGE);
		}
	}

	@PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamMessage(@RequestBody final AiSendMessageRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final SseEmitter emitter = new SseEmitter(300_000L);
		emitter.onTimeout(emitter::complete);
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			completeStreamError(emitter, "Sesión no válida.");
			return emitter;
		}
		if (request == null) {
			completeStreamError(emitter, "Solicitud no válida.");
			return emitter;
		}
		try {
			aiChatRateLimiter.executeMessage(nutritionistId, () -> {
				chatService.streamMessage(nutritionistId, request, emitter);
				return null;
			});
		}
		catch (final RequestNotPermitted ex) {
			recordChatRateLimited("AI chat stream rate limit exceeded");
			completeStreamError(emitter, AiChatRateLimiter.RATE_LIMIT_USER_MESSAGE);
		}
		catch (final RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI chat stream failed unexpectedly", ex);
			}
			completeStreamError(emitter, "No se pudo completar la solicitud.");
		}
		return emitter;
	}

	@PostMapping("/message/edit")
	public ResponseEntity<Map<String, Object>> editMessage(@RequestBody final AiEditMessageRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		if (request == null) {
			return errorResponse(HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION, "Solicitud no válida.");
		}
		try {
			final AiEditResubmitResult result = aiChatRateLimiter.executeMessage(nutritionistId,
					() -> chatService.editAndResubmitMessage(nutritionistId, request));
			final Map<String, Object> response = successBody();
			putOrchestrationFields(response, result.orchestration());
			response.put("truncatedMessageCount", result.truncatedMessageCount());
			response.put("discardedDraftIds", result.discardedDraftIds());
			return ResponseEntity.ok(response);
		}
		catch (final AiChatException ex) {
			return errorResponse(ex);
		}
		catch (final AiOrchestrationException ex) {
			return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, AiToolErrorCode.VALIDATION, ex.getMessage());
		}
		catch (final OpenAiClientException ex) {
			return mapOpenAiException(ex);
		}
		catch (final RequestNotPermitted ex) {
			recordChatRateLimited("AI chat edit rate limit exceeded");
			return errorResponse(HttpStatus.TOO_MANY_REQUESTS, AiToolErrorCode.RATE_LIMIT,
					AiChatRateLimiter.RATE_LIMIT_USER_MESSAGE);
		}
	}

	@PostMapping(value = "/message/edit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamEditMessage(@RequestBody final AiEditMessageRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final SseEmitter emitter = new SseEmitter(300_000L);
		emitter.onTimeout(emitter::complete);
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			completeStreamError(emitter, "Sesión no válida.");
			return emitter;
		}
		if (request == null) {
			completeStreamError(emitter, "Solicitud no válida.");
			return emitter;
		}
		try {
			aiChatRateLimiter.executeMessage(nutritionistId, () -> {
				chatService.streamEditMessage(nutritionistId, request, emitter);
				return null;
			});
		}
		catch (final RequestNotPermitted ex) {
			recordChatRateLimited("AI chat edit stream rate limit exceeded");
			completeStreamError(emitter, AiChatRateLimiter.RATE_LIMIT_USER_MESSAGE);
		}
		catch (final RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI chat edit stream failed unexpectedly", ex);
			}
			completeStreamError(emitter, "No se pudo completar la solicitud.");
		}
		return emitter;
	}

	private static void completeStreamError(final SseEmitter emitter, final String message) {
		try {
			AiChatSseSupport.sendError(emitter, message);
		}
		catch (Exception ex) {
			emitter.completeWithError(ex);
			return;
		}
		AiChatSseSupport.completeQuietly(emitter);
	}

	@GetMapping("/{threadId}")
	public ResponseEntity<Map<String, Object>> getThread(@PathVariable @NonNull final Long threadId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		try {
			final AiChatThreadDetail detail = chatService.getThread(nutritionistId, threadId);
			final Map<String, Object> response = successBody();
			response.put("threadId", detail.threadId());
			response.put("title", detail.title());
			response.put("patientId", detail.patientId());
			response.put("clinicId", detail.clinicId());
			response.put("createdAt", detail.createdAt());
			response.put("updatedAt", detail.updatedAt());
			response.put("messages", toMessageMaps(detail.messages()));
			return ResponseEntity.ok(response);
		}
		catch (final AiChatException ex) {
			return errorResponse(ex);
		}
	}

	@GetMapping("/{threadId}/drafts")
	public ResponseEntity<Map<String, Object>> listDrafts(@PathVariable @NonNull final Long threadId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		try {
			final AiChatDraftList draftList = chatService.listDrafts(nutritionistId, threadId);
			final Map<String, Object> response = successBody();
			response.put("threadId", draftList.threadId());
			response.put("drafts", toDraftMaps(draftList.drafts()));
			return ResponseEntity.ok(response);
		}
		catch (final AiChatException ex) {
			return errorResponse(ex);
		}
	}

	private void recordChatRateLimited(final String debugMessage) {
		aiUsageMetrics.recordChatRateLimited();
		if (log.isDebugEnabled()) {
			log.debug(debugMessage);
		}
	}

	private static String nutritionistId(final OidcUser principal) {
		if (principal == null || principal.getSubject() == null || principal.getSubject().isBlank()) {
			return null;
		}
		return principal.getSubject();
	}

	private static ResponseEntity<Map<String, Object>> unauthorized() {
		return errorResponse(HttpStatus.UNAUTHORIZED, AiToolErrorCode.VALIDATION, "Sesión no válida.");
	}

	private static Map<String, Object> successBody() {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", true);
		return body;
	}

	private static ResponseEntity<Map<String, Object>> errorResponse(final AiChatException ex) {
		return errorResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
	}

	private static ResponseEntity<Map<String, Object>> errorResponse(final HttpStatus status,
			final AiToolErrorCode errorCode, final String message) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("errorCode", errorCode.name());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}

	private static ResponseEntity<Map<String, Object>> mapOpenAiException(final OpenAiClientException ex) {
		final HttpStatus status = switch (ex.getKind()) {
			case NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
			case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
			case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
			default -> HttpStatus.BAD_GATEWAY;
		};
		return errorResponse(status, AiToolErrorCode.VALIDATION, ex.getUserMessage());
	}

	private static Map<String, Object> tokenUsageMap(final OpenAiTokenUsage usage) {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("promptTokens", usage.promptTokens());
		map.put("completionTokens", usage.completionTokens());
		map.put("totalTokens", usage.totalTokens());
		return map;
	}

	private static void putOrchestrationFields(final Map<String, Object> response, final AiOrchestrationResult result) {
		response.put("threadId", result.threadId());
		response.put("assistantMessageId", result.assistantMessage().getId());
		response.put("content", result.assistantMessage().getContent());
		response.put("toolCallsExecuted", result.toolCallsExecuted());
		if (result.tokenUsage() != null) {
			response.put("tokenUsage", tokenUsageMap(result.tokenUsage()));
		}
	}

	private static List<Map<String, Object>> toMessageMaps(final List<AiChatMessageView> messages) {
		final List<Map<String, Object>> maps = new ArrayList<>();
		for (final AiChatMessageView message : messages) {
			final Map<String, Object> map = new LinkedHashMap<>();
			map.put("id", message.id());
			map.put("role", message.role().name());
			map.put("content", message.content());
			map.put("createdAt", message.createdAt());
			maps.add(map);
		}
		return maps;
	}

	private static List<Map<String, Object>> toDraftMaps(final List<AiChatDraftSummary> drafts) {
		final List<Map<String, Object>> maps = new ArrayList<>();
		for (final AiChatDraftSummary draft : drafts) {
			final Map<String, Object> map = new LinkedHashMap<>();
			map.put("draftId", draft.draftId());
			map.put("draftType", draft.draftType().name());
			map.put("status", draft.status().name());
			map.put("summary", draft.summary());
			map.put("createdAt", draft.createdAt());
			maps.add(map);
		}
		return maps;
	}

}
