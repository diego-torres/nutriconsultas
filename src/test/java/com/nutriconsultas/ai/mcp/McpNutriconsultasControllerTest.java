package com.nutriconsultas.ai.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.nutriconsultas.ai.AiErrorMessages;

@ExtendWith(MockitoExtension.class)
class McpNutriconsultasControllerTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private McpNutriconsultasController controller;

	@Mock
	private McpToolDispatchService dispatchService;

	@Test
	void unauthenticatedRequestReturns401() {
		final ResponseEntity<Map<String, Object>> response = controller
			.handle(Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 1), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		@SuppressWarnings("unchecked")
		final Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
		assertThat(error).containsEntry("code", McpJsonRpcResponses.ERROR_UNAUTHORIZED)
			.containsEntry("message", AiErrorMessages.INVALID_SESSION);
	}

	@Test
	void authenticatedToolsListReturnsOk() {
		when(dispatchService.handle(NUTRITIONIST_ID,
				Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 1, "params", Map.of())))
			.thenReturn(Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of("tools", List.of())));

		final ResponseEntity<Map<String, Object>> response = controller.handle(
				Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 1, "params", Map.of()),
				principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsKey("result");
	}

	@Test
	void accessExceptionMapsToHttpStatus() {
		when(dispatchService.handle(NUTRITIONIST_ID,
				Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 1, "params", Map.of())))
			.thenThrow(new McpAccessException(HttpStatus.FORBIDDEN, "Sin acceso"));

		final ResponseEntity<Map<String, Object>> response = controller.handle(
				Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 1, "params", Map.of()),
				principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	private static DefaultOidcUser principal(final String subject) {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", subject));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
