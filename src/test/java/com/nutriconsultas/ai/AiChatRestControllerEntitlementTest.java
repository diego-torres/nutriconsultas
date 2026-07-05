package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

@ExtendWith(MockitoExtension.class)
class AiChatRestControllerEntitlementTest {

	private static final String NUTRITIONIST_ID = "auth0|profesional-user";

	@InjectMocks
	private AiChatRestController controller;

	@Mock
	private AiChatService chatService;

	@Mock
	private AiChatRateLimiter aiChatRateLimiter;

	@Test
	void sendMessagePropagatesSubscriptionDenial() throws Exception {
		when(aiChatRateLimiter.executeMessage(eq(NUTRITIONIST_ID), any())).thenAnswer(invocation -> {
			final Callable<?> callable = invocation.getArgument(1);
			return callable.call();
		});
		when(chatService.sendMessage(eq(NUTRITIONIST_ID), org.mockito.ArgumentMatchers.anyLong(), eq("Hola"),
				org.mockito.ArgumentMatchers.any()))
			.thenThrow(new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED));

		assertThatThrownBy(() -> controller.sendMessage(new AiSendMessageRequest(1L, "Hola"), principal()))
			.isInstanceOf(SubscriptionLimitExceededException.class);
	}

	@Test
	void startChatPropagatesSubscriptionDenial() {
		when(chatService.startThread(eq(NUTRITIONIST_ID), org.mockito.ArgumentMatchers.isNull(),
				org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
				org.mockito.ArgumentMatchers.any()))
			.thenThrow(new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED));

		assertThatThrownBy(
				() -> controller.startChat(new AiStartChatRequest(null, null, null, null, null), principal()))
			.isInstanceOf(SubscriptionLimitExceededException.class);
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
