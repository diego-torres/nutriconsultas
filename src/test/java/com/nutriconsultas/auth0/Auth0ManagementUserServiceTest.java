package com.nutriconsultas.auth0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class Auth0ManagementUserServiceTest {

	private static final String DOMAIN = "tenant.example.com";

	private Auth0ManagementTokenProvider tokenProvider;

	private MockRestServiceServer mockServer;

	private Auth0ManagementUserServiceImpl service;

	@BeforeEach
	void setUp() {
		tokenProvider = mock(Auth0ManagementTokenProvider.class);
		when(tokenProvider.isConfigured()).thenReturn(true);
		when(tokenProvider.getDomain()).thenReturn(DOMAIN);
		when(tokenProvider.obtainToken()).thenReturn("mgmt-token");
		final RestClient.Builder restClientBuilder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		service = new Auth0ManagementUserServiceImpl(restClientBuilder, tokenProvider, false, 0);
	}

	@Test
	void findUserByAppleSubjectUsesDirectUserLookup() {
		mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/apple%7C001234.abc"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
					{
					  "user_id": "apple|001234.abc",
					  "email": "relay@privaterelay.appleid.com",
					  "app_metadata": {},
					  "identities": [{"provider":"apple","user_id":"001234.abc"}]
					}
					""", MediaType.APPLICATION_JSON));

		final Optional<Auth0ManagementUser> user = service.findUserByAppleSubject("001234.abc");

		assertThat(user).isPresent();
		assertThat(user.get().userId()).isEqualTo("apple|001234.abc");
		mockServer.verify();
	}

	@Test
	void updateAppMetadataPatchesUser() {
		mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/apple%7C001234.abc"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess());

		service.updateAppMetadata("apple|001234.abc", java.util.Map.of("apple_signin_seen", true));

		mockServer.verify();
	}

	@Test
	void deleteUserIsDisabledByDefault() {
		assertThatThrownBy(() -> service.deleteUser("apple|001234.abc")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("disabled");
	}

	@Test
	void blockUserInAppMetadataSetsBlockedFlags() {
		mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/apple%7C001234.abc"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess());

		service.blockUserInAppMetadata("apple|001234.abc");

		mockServer.verify();
	}

}
