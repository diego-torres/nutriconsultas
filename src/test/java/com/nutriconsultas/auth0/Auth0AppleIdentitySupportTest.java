package com.nutriconsultas.auth0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Auth0AppleIdentitySupportTest {

	@Test
	void toAuth0UserIdUsesApplePrefix() {
		assertThat(Auth0AppleIdentitySupport.toAuth0UserId("001234.abc")).isEqualTo("apple|001234.abc");
	}

	@Test
	void isAppleAuth0UserIdDetectsAppleProvider() {
		assertThat(Auth0AppleIdentitySupport.isAppleAuth0UserId("apple|001234.abc")).isTrue();
		assertThat(Auth0AppleIdentitySupport.isAppleAuth0UserId("google-oauth2|123")).isFalse();
	}

	@Test
	void toAuth0UserIdReturnsNullForBlankSubject() {
		assertThat(Auth0AppleIdentitySupport.toAuth0UserId("")).isNull();
		assertThat(Auth0AppleIdentitySupport.toAuth0UserId(null)).isNull();
	}

}
