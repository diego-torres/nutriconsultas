package com.nutriconsultas.ai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/nutritionist/ai/drafts")
@Slf4j
public class AiDraftRestController {

	private final AiDraftAcceptanceService draftAcceptanceService;

	private final AiDraftLifecycleService draftLifecycleService;

	private final AiDraftPreviewService draftPreviewService;

	public AiDraftRestController(final AiDraftAcceptanceService draftAcceptanceService,
			final AiDraftLifecycleService draftLifecycleService, final AiDraftPreviewService draftPreviewService) {
		this.draftAcceptanceService = draftAcceptanceService;
		this.draftLifecycleService = draftLifecycleService;
		this.draftPreviewService = draftPreviewService;
	}

	@GetMapping("/{draftId}")
	public ResponseEntity<Map<String, Object>> getDraftPreview(@PathVariable @NonNull final Long draftId,
			@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			return errorResponse(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
		}
		try {
			final AiDraftPreviewView preview = draftPreviewService.getPreview(draftId, principal.getSubject());
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("draftId", preview.draftId());
			body.put("threadId", preview.threadId());
			body.put("draftType", preview.draftType().name());
			body.put("status", preview.status().name());
			body.put("draftTypeLabel", preview.draftTypeLabel());
			body.put("reviewLabel", preview.reviewLabel());
			body.put("title", preview.title());
			body.put("summary", preview.summary());
			body.put("portions", preview.portions());
			body.put("dayCount", preview.dayCount());
			body.put("nutrients", preview.nutrients());
			body.put("ingredients", preview.ingredients());
			body.put("mealSlots", preview.mealSlots());
			body.put("preparationSteps", preview.preparationSteps());
			body.put("assumptions", preview.assumptions());
			body.put("warnings", preview.warnings());
			body.put("validationSummary", preview.validationSummary());
			return ResponseEntity.ok(body);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	@PostMapping("/{draftId}/accept")
	public ResponseEntity<Map<String, Object>> acceptDraft(@PathVariable @NonNull final Long draftId,
			@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			return errorResponse(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
		}
		try {
			final AiDraftAcceptanceResult result = draftAcceptanceService.accept(draftId, principal.getSubject(),
					principal);
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("draftId", result.draftId());
			body.put("draftType", result.draftType().name());
			body.put("status", result.status().name());
			body.put("createdEntityType", result.createdEntityType().name());
			body.put("createdEntityId", result.createdEntityId());
			body.put("summary", result.summary());
			return ResponseEntity.ok(body);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	@PostMapping("/{draftId}/discard")
	public ResponseEntity<Map<String, Object>> discardDraft(@PathVariable @NonNull final Long draftId,
			@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			return errorResponse(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
		}
		try {
			final AiGeneratedDraft discarded = draftLifecycleService.discardDraft(draftId, principal.getSubject());
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("draftId", discarded.getId());
			body.put("draftType", discarded.getDraftType().name());
			body.put("status", discarded.getStatus().name());
			return ResponseEntity.ok(body);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	private static ResponseEntity<Map<String, Object>> mapLifecycleException(final AiDraftLifecycleException ex) {
		final HttpStatus status;
		final String errorCode;
		if (ex.getMessage() != null && ex.getMessage().contains("Borrador no encontrado")) {
			status = HttpStatus.NOT_FOUND;
			errorCode = AiToolErrorCode.NOT_FOUND.name();
		}
		else if (ex.getMessage() != null && ex.getMessage().contains("Conversación no encontrada")) {
			status = HttpStatus.NOT_FOUND;
			errorCode = AiToolErrorCode.NOT_FOUND.name();
		}
		else {
			status = HttpStatus.BAD_REQUEST;
			errorCode = AiToolErrorCode.VALIDATION.name();
		}
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("errorCode", errorCode);
		body.put("message", ex.getMessage());
		return ResponseEntity.status(status).body(body);
	}

	private static ResponseEntity<Map<String, Object>> errorResponse(final HttpStatus status, final String message) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("errorCode", AiToolErrorCode.VALIDATION.name());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}

}
