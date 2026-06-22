package com.nutriconsultas.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@ExtendWith(MockitoExtension.class)
class NutritionistOAuth2AuthorizationRequestResolverTest {

	@Mock
	private ClientRegistrationRepository clientRegistrationRepository;

	private NutritionistOAuth2AuthorizationRequestResolver resolver;

	@BeforeEach
	void setUp() {
		whenRegistration();
		resolver = new NutritionistOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
	}

	@Test
	void defaultAuthorizationUsesLoginScreenHint() {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/auth0");
		final OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request, "auth0");

		assertThat(authorizationRequest).isNotNull();
		assertThat(authorizationRequest.getAdditionalParameters()).containsEntry("screen_hint", "login");
	}

	@Test
	void signupParameterUsesSignupScreenHint() {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/auth0");
		request.setParameter("signup", "true");
		final OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request, "auth0");

		assertThat(authorizationRequest).isNotNull();
		assertThat(authorizationRequest.getAdditionalParameters()).containsEntry("screen_hint", "signup");
	}

	private void whenRegistration() {
		final ClientRegistration registration = ClientRegistration.withRegistrationId("auth0")
			.clientId("client-id")
			.clientSecret("secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri("https://example.com/authorize")
			.tokenUri("https://example.com/token")
			.userInfoUri("https://example.com/userinfo")
			.jwkSetUri("https://example.com/jwks")
			.clientName("Auth0")
			.build();
		org.mockito.Mockito.when(clientRegistrationRepository.findByRegistrationId("auth0")).thenReturn(registration);
	}

}
