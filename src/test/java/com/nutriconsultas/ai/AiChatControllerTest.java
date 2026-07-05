package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

	@InjectMocks
	private AiChatController controller;

	@Mock
	private AiProperties aiProperties;

	@Mock
	private AiEntitlementGuard aiEntitlementGuard;

	@Mock
	private com.nutriconsultas.subscription.SubscriptionErrorResponses subscriptionErrorResponses;

	private static final String USER_ID = "auth0|plus-user";

	@Test
	void chatHomeReturnsViewWhenEnabledAndEntitled() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.chatHome(null, model, principal());

		assertThat(view).isEqualTo("sbadmin/ai/chat");
		assertThat(model.getAttribute("activeMenu")).isEqualTo("ai");
		assertThat(model.getAttribute("initialThreadId")).isNull();
	}

	@Test
	void chatHomePassesInitialThreadIdWhenRequested() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.chatHome(42L, model, principal());

		assertThat(view).isEqualTo("sbadmin/ai/chat");
		assertThat(model.getAttribute("initialThreadId")).isEqualTo(42L);
	}

	@Test
	void chatHomeReturnsNotFoundWhenDisabled() {
		when(aiProperties.isEnabled()).thenReturn(false);

		assertThatThrownBy(() -> controller.chatHome(null, new ExtendedModelMap(), principal()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(404);
	}

	@Test
	void chatHomeReturnsForbiddenWhenPlanDenied() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final SubscriptionLimitExceededException denied = new SubscriptionLimitExceededException(
				SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED);
		org.mockito.Mockito.doThrow(denied).when(aiEntitlementGuard).assertCanUseAiAssistant(USER_ID);
		when(subscriptionErrorResponses.resolve(denied)).thenReturn("El asistente de IA nutricional requiere el plan Plus o Consultorio.");

		assertThatThrownBy(() -> controller.chatHome(null, new ExtendedModelMap(), principal()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.FORBIDDEN.value());
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", USER_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
