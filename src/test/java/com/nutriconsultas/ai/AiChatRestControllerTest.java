package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@ExtendWith(MockitoExtension.class)
class AiChatRestControllerTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final String OTHER_NUTRITIONIST_ID = "auth0|nutritionist-b";

	@InjectMocks
	private AiChatRestController controller;

	@Mock
	private AiChatService chatService;

	@Mock
	private AiChatRateLimiter aiChatRateLimiter;

	@Test
	void startChatReturnsCreatedThread() {
		final AiChatThread thread = new AiChatThread();
		thread.setId(5L);
		thread.setTitle("Menú semanal");
		thread.setNutritionistId(NUTRITIONIST_ID);
		thread.setCreatedAt(Instant.parse("2026-06-30T12:00:00Z"));
		thread.setUpdatedAt(Instant.parse("2026-06-30T12:00:00Z"));
		when(chatService.startThread(eq(NUTRITIONIST_ID), eq("Menú semanal"), eq(10L), eq(null), any()))
			.thenReturn(thread);

		final ResponseEntity<Map<String, Object>> response = controller
			.startChat(new AiStartChatRequest("Menú semanal", 10L, null, null, null), principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).containsEntry("success", true).containsEntry("threadId", 5L);
	}

	@Test
	void sendMessageReturnsAssistantReply() throws Exception {
		final AiChatMessage assistant = new AiChatMessage();
		assistant.setId(99L);
		assistant.setRole(AiChatMessageRole.ASSISTANT);
		assistant.setContent("Aquí tienes una sugerencia.");
		when(chatService.sendMessage(eq(NUTRITIONIST_ID), eq(5L), eq("Hola"), any()))
			.thenReturn(new AiOrchestrationResult(5L, assistant, 1, new OpenAiTokenUsage(10, 8, 18)));
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});

		final ResponseEntity<Map<String, Object>> response = controller
			.sendMessage(new AiSendMessageRequest(5L, "Hola"), principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("assistantMessageId", 99L).containsEntry("toolCallsExecuted", 1);
		verify(chatService).sendMessage(eq(NUTRITIONIST_ID), eq(5L), eq("Hola"), any());
	}

	@Test
	void getThreadReturnsMessages() {
		final Instant now = Instant.parse("2026-06-30T12:00:00Z");
		final AiChatThreadDetail detail = new AiChatThreadDetail(5L, "Menú", 10L, null, now, now,
				List.of(new AiChatMessageView(1L, AiChatMessageRole.USER, "Hola", now)));
		when(chatService.getThread(NUTRITIONIST_ID, 5L)).thenReturn(detail);

		final ResponseEntity<Map<String, Object>> response = controller.getThread(5L, principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("threadId", 5L).containsKey("messages");
	}

	@Test
	void listDraftsReturnsSummaries() {
		final Instant now = Instant.parse("2026-06-30T12:00:00Z");
		when(chatService.listDrafts(NUTRITIONIST_ID, 5L)).thenReturn(new AiChatDraftList(5L,
				List.of(new AiChatDraftSummary(11L, AiDraftType.DISH, AiDraftStatus.DRAFT, "Borrador", now))));

		final ResponseEntity<Map<String, Object>> response = controller.listDrafts(5L, principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("threadId", 5L).containsKey("drafts");
	}

	@Test
	void getThreadReturnsNotFoundForOtherNutritionist() {
		when(chatService.getThread(OTHER_NUTRITIONIST_ID, 5L)).thenThrow(new AiChatException(HttpStatus.NOT_FOUND,
				AiToolErrorCode.NOT_FOUND, "No se encontró la conversación."));

		final ResponseEntity<Map<String, Object>> response = controller.getThread(5L, principal(OTHER_NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("success", false);
	}

	@Test
	void sendMessageMapsOpenAiRateLimit() throws Exception {
		when(chatService.sendMessage(any(), eq(5L), any(), any())).thenThrow(
				new OpenAiClientException(OpenAiClientException.ErrorKind.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS,
						"El servicio de IA está saturado. Intenta de nuevo en unos minutos.", "rate limit", null));
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});

		final ResponseEntity<Map<String, Object>> response = controller
			.sendMessage(new AiSendMessageRequest(5L, "Hola"), principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getBody()).containsEntry("message",
				"El servicio de IA está saturado. Intenta de nuevo en unos minutos.");
	}

	@Test
	void sendMessageReturns429WhenAppRateLimitExceeded() {
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenThrow(RequestNotPermitted
			.createRequestNotPermitted(io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("aiChatMessage")));

		final ResponseEntity<Map<String, Object>> response = controller
			.sendMessage(new AiSendMessageRequest(5L, "Hola"), principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getBody()).containsEntry("success", false)
			.containsEntry("errorCode", AiToolErrorCode.RATE_LIMIT.name())
			.containsEntry("message", AiChatRateLimiter.RATE_LIMIT_USER_MESSAGE);
	}

	@Test
	void streamMessageReturnsSseEmitter() throws Exception {
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});

		final SseEmitter emitter = controller.streamMessage(new AiSendMessageRequest(5L, "Hola"),
				principal(NUTRITIONIST_ID));

		assertThat(emitter).isNotNull();
		verify(chatService).streamMessage(eq(NUTRITIONIST_ID), any(AiSendMessageRequest.class), any(SseEmitter.class));
	}

	@Test
	void editMessageReturnsAssistantReply() throws Exception {
		final AiChatMessage assistant = new AiChatMessage();
		assistant.setId(99L);
		assistant.setRole(AiChatMessageRole.ASSISTANT);
		assistant.setContent("Respuesta editada.");
		when(chatService.editAndResubmitMessage(eq(NUTRITIONIST_ID), any(AiEditMessageRequest.class)))
			.thenReturn(new AiEditResubmitResult(new AiOrchestrationResult(5L, assistant, 0, null), 2, List.of(11L)));
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});

		final ResponseEntity<Map<String, Object>> response = controller.editMessage(
				new AiEditMessageRequest(5L, 10L, "Texto editado", null, null, null), principal(NUTRITIONIST_ID));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("assistantMessageId", 99L)
			.containsEntry("truncatedMessageCount", 2)
			.containsEntry("discardedDraftIds", List.of(11L));
	}

	@Test
	void streamEditMessageReturnsSseEmitter() throws Exception {
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});

		final SseEmitter emitter = controller.streamEditMessage(
				new AiEditMessageRequest(5L, 10L, "Texto editado", null, null, null), principal(NUTRITIONIST_ID));

		assertThat(emitter).isNotNull();
		verify(chatService).streamEditMessage(eq(NUTRITIONIST_ID), any(AiEditMessageRequest.class),
				any(SseEmitter.class));
	}

	@Test
	void startChatRequiresAuthentication() {
		final ResponseEntity<Map<String, Object>> response = controller.startChat(null, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private static DefaultOidcUser principal(final String subject) {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", subject));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
