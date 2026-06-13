package com.nutriconsultas.mobile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.nutriconsultas.security.TestMobileJwtDecoderConfig;

public final class MobileIntegrationTestJwt {

	private static final String TEST_ISSUER = "https://dev-imd1udg26uvzvfto.us.auth0.com/";

	private static final String TEST_AUDIENCE = "https://api.nutriconsultas.test/mobile";

	private MobileIntegrationTestJwt() {
	}

	public static RequestPostProcessor mobileJwt(final String subject) {
		final String tokenValue = TestMobileJwtDecoderConfig.TOKEN_PREFIX + subject;
		return jwt().jwt(j -> j.tokenValue(tokenValue)
			.subject(subject)
			.issuer(TEST_ISSUER)
			.claim("aud", List.of(TEST_AUDIENCE)));
	}

}
