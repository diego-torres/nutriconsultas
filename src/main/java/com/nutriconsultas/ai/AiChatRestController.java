package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/nutritionist/ai/chat")
@Slf4j
public class AiChatRestController {

	private final AiChatService chatService;

	public AiChatRestController(final AiChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping("/start")
	public ResponseEntity<Map<String, Object>> startChat(
			@RequestBody(required = false) final AiStartChatRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = nutritionistId(principal);
		if (nutritionistId == null) {
			return unauthorized();
		}
		final AiStartChatRequest body = request != null ? request : new AiStartChatRequest(null, null, null);
		try {
			final AiChatThread thread = chatService.startThread(nutritionistId, body.title(), body.patientId(),
					body.clinicId());
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
			final AiOrchestrationResult result = chatService.sendMessage(nutritionistId, request.threadId(),
					request.message());
			final Map<String, Object> response = successBody();
			response.put("threadId", result.threadId());
			response.put("assistantMessageId", result.assistantMessage().getId());
			response.put("content", result.assistantMessage().getContent());
			response.put("toolCallsExecuted", result.toolCallsExecuted());
			if (result.tokenUsage() != null) {
				response.put("tokenUsage", tokenUsageMap(result.tokenUsage()));
			}
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
