package com.nutriconsultas.ai.mcp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.ai.AiErrorMessages;

import lombok.extern.slf4j.Slf4j;

/**
 * MCP-compatible HTTP endpoint for nutrition tool dispatch (#394).
 */
@RestController
@RequestMapping("/mcp/nutriconsultas")
@Slf4j
public final class McpNutriconsultasController {

	private final McpToolDispatchService dispatchService;

	public McpNutriconsultasController(final McpToolDispatchService dispatchService) {
		this.dispatchService = dispatchService;
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> handle(@RequestBody(required = false) final Map<String, Object> body,
			@AuthenticationPrincipal final OidcUser principal) {
		final String nutritionistId = principal != null ? principal.getSubject() : null;
		if (!StringUtils.hasText(nutritionistId)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(McpJsonRpcResponses.error(null, McpJsonRpcResponses.ERROR_UNAUTHORIZED,
						AiErrorMessages.INVALID_SESSION));
		}
		if (body == null || body.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(McpJsonRpcResponses.error(null, McpJsonRpcResponses.ERROR_INVALID_REQUEST,
						"Solicitud JSON-RPC no válida."));
		}
		try {
			return ResponseEntity.ok(dispatchService.handle(nutritionistId, body));
		}
		catch (final McpAccessException ex) {
			if (log.isDebugEnabled()) {
				log.debug("MCP access denied status={}", ex.getStatus().value());
			}
			return ResponseEntity.status(ex.getStatus())
				.body(McpJsonRpcResponses.error(body.get("id"), mapHttpStatusToJsonRpcCode(ex.getStatus()),
						ex.getUserMessage()));
		}
	}

	private static int mapHttpStatusToJsonRpcCode(final HttpStatus status) {
		if (status == HttpStatus.FORBIDDEN) {
			return McpJsonRpcResponses.ERROR_FORBIDDEN;
		}
		if (status == HttpStatus.NOT_FOUND) {
			return McpJsonRpcResponses.ERROR_NOT_FOUND;
		}
		if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			return McpJsonRpcResponses.ERROR_UNAVAILABLE;
		}
		return McpJsonRpcResponses.ERROR_SERVER;
	}

}
