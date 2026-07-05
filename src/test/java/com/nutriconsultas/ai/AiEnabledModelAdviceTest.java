package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AiEnabledModelAdviceTest {

	@InjectMocks
	private AiEnabledModelAdvice advice;

	@Mock
	private AiProperties aiProperties;

	@Mock
	private AiEntitlementGuard aiEntitlementGuard;

	@Test
	void addsAiAssistantAvailableWhenFlagAndEntitlementOn() {
		when(aiProperties.isEnabled()).thenReturn(true);
		when(aiEntitlementGuard.canUseAiAssistant(USER_ID)).thenReturn(true);
		final Model model = new ExtendedModelMap();

		advice.addAiEnabledFlag(model, principal());

		assertThat(model.getAttribute("aiEnabled")).isEqualTo(true);
		assertThat(model.getAttribute("aiAssistantAvailable")).isEqualTo(true);
		assertThat(model.asMap()).doesNotContainKey("openaiApiKey");
	}

	@Test
	void aiAssistantUnavailableWhenEntitlementMissing() {
		when(aiProperties.isEnabled()).thenReturn(true);
		when(aiEntitlementGuard.canUseAiAssistant(USER_ID)).thenReturn(false);
		final Model model = new ExtendedModelMap();

		advice.addAiEnabledFlag(model, principal());

		assertThat(model.getAttribute("aiEnabled")).isEqualTo(true);
		assertThat(model.getAttribute("aiAssistantAvailable")).isEqualTo(false);
	}

	@Test
	void aiAssistantUnavailableWhenFeatureFlagOff() {
		when(aiProperties.isEnabled()).thenReturn(false);
		final Model model = new ExtendedModelMap();

		advice.addAiEnabledFlag(model, principal());

		assertThat(model.getAttribute("aiEnabled")).isEqualTo(false);
		assertThat(model.getAttribute("aiAssistantAvailable")).isEqualTo(false);
	}

	private static final String USER_ID = "auth0|plus-user";

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", USER_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
